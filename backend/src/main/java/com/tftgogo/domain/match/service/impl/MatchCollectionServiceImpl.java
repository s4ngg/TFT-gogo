package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.queue.RiotQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MatchCollectionServiceImpl implements MatchCollectionService {

    private static final Logger logger = LogManager.getLogger(MatchCollectionServiceImpl.class);
    private static final Set<Integer> VALID_QUEUE_IDS = Set.of(1090, 1100);
    private static final long FETCH_TIMEOUT_SECONDS = 60L;

    // MatchDto는 @Getter만 있고 setter 없음 → FIELD visibility로 직접 접근
    private static final ObjectMapper CACHE_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    private final RiotApiClient riotApiClient;
    private final RiotQueue riotQueue;
    private final CachedMatchRepository cachedMatchRepository;
    private final Executor matchCollectionExecutor;

    // 진행 중인 수집 작업 추적 (puuid → 진행 여부)
    private final ConcurrentHashMap<String, Boolean> inProgressMap = new ConcurrentHashMap<>();

    public MatchCollectionServiceImpl(
            RiotApiClient riotApiClient,
            RiotQueue riotQueue,
            CachedMatchRepository cachedMatchRepository,
            @Qualifier("matchCollectionExecutor") Executor matchCollectionExecutor) {
        this.riotApiClient = riotApiClient;
        this.riotQueue = riotQueue;
        this.cachedMatchRepository = cachedMatchRepository;
        this.matchCollectionExecutor = matchCollectionExecutor;
    }

    @Override
    public List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count,
                                                     Function<String, String> traitIconFn,
                                                     Function<String, String> traitNameFn,
                                                     Function<String, String> itemIconFn) {
        // DB에 충분한 데이터가 있으면 즉시 반환
        List<CachedMatch> existing = cachedMatchRepository.findByParticipantPuuid(
                puuid, PageRequest.of(0, start + count));
        if (existing.size() >= start + count) {
            return toSummonerMatchItemDtoList(puuid, existing.subList(start, start + count), traitIconFn, traitNameFn, itemIconFn);
        }

        // Riot API로 matchId 목록 조회
        List<String> matchIds = fetchMatchIds(puuid, start, count);
        if (matchIds.isEmpty()) return List.of();

        Set<String> cachedIds = new HashSet<>(cachedMatchRepository.findMatchIdsByMatchIdIn(matchIds));
        List<String> toFetch = matchIds.stream()
                .filter(id -> !cachedIds.contains(id))
                .collect(Collectors.toList());

        if (!toFetch.isEmpty()) {
            int fastTarget = Math.max(0, count - cachedIds.size());
            collectInBackground(puuid, toFetch, fastTarget);
        }

        return buildResult(puuid, matchIds, traitIconFn, traitNameFn, itemIconFn);
    }

    private List<String> fetchMatchIds(String puuid, int start, int count) {
        try {
            CompletableFuture<List<String>> rankedFuture =
                    riotQueue.submitForeground(() -> riotApiClient.getMatchIds(puuid, count, start, 1100));
            CompletableFuture<List<String>> normalFuture =
                    riotQueue.submitForeground(() -> riotApiClient.getMatchIds(puuid, count, start, 1090));

            List<String> ranked = rankedFuture.get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<String> normal = normalFuture.get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Set<String> merged = new LinkedHashSet<>(ranked);
            merged.addAll(normal);
            return new ArrayList<>(merged);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException be) throw be;
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        } catch (TimeoutException e) {
            logger.error("matchId 목록 조회 타임아웃: puuid={}", puuid, e);
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        } catch (Exception e) {
            logger.error("matchId 목록 조회 실패: puuid={}", puuid, e);
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
    }

    // requestedFast개 수집 완료까지 블로킹, 나머지는 백그라운드 계속 진행
    private void collectInBackground(String puuid, List<String> toFetch, int requestedFast) {
        int fastTarget = Math.min(requestedFast, toFetch.size());
        CountDownLatch latch = new CountDownLatch(fastTarget);
        AtomicInteger completedCount = new AtomicInteger(0);

        inProgressMap.put(puuid, Boolean.TRUE);

        for (String matchId : toFetch) {
            riotQueue.submit(() -> riotApiClient.getMatch(matchId))
                    .thenApplyAsync(matchDto -> {
                        persistMatch(matchId, matchDto);
                        return null;
                    }, matchCollectionExecutor)
                    .whenComplete((ignored, ex) -> {
                        if (ex != null) {
                            logger.warn("매치 수집 실패, 건너뜀: matchId={}", matchId, ex);
                        }
                        int n = completedCount.incrementAndGet();
                        if (n <= fastTarget) {
                            latch.countDown();
                        }
                        if (n >= toFetch.size()) {
                            inProgressMap.remove(puuid);
                            logger.info("매치 수집 완료: puuid={}, total={}", puuid, toFetch.size());
                        }
                    });
        }

        try {
            boolean finished = latch.await(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                logger.warn("매치 수집 타임아웃 (첫 {}개): puuid={}", fastTarget, puuid);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void persistMatch(String matchId, MatchDto matchDto) {
        MatchDto.MatchInfoDto info = matchDto.getInfo();
        if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) return;
        if (cachedMatchRepository.existsById(matchId)) return;

        Set<String> puuids = info.getParticipants() == null ? Set.of() :
                info.getParticipants().stream()
                        .map(MatchDto.ParticipantDto::getPuuid)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        try {
            String json = CACHE_MAPPER.writeValueAsString(matchDto);
            cachedMatchRepository.save(CachedMatch.builder()
                    .matchId(matchId)
                    .queueId(info.getQueue_id())
                    .gameDatetime(info.getGame_datetime())
                    .matchJson(json)
                    .createdAt(LocalDateTime.now())
                    .participantPuuids(puuids)
                    .build());
        } catch (Exception e) {
            logger.error("매치 DB 저장 실패: matchId={}", matchId, e);
        }
    }

    private List<SummonerMatchItemDto> buildResult(String puuid, List<String> matchIds,
                                                    Function<String, String> traitIconFn,
                                                    Function<String, String> traitNameFn,
                                                    Function<String, String> itemIconFn) {
        Map<String, CachedMatch> cacheMap = cachedMatchRepository.findAllById(matchIds)
                .stream()
                .collect(Collectors.toMap(CachedMatch::getMatchId, m -> m));

        return matchIds.stream()
                .filter(cacheMap::containsKey)
                .map(id -> toDto(puuid, id, cacheMap.get(id), traitIconFn, traitNameFn, itemIconFn))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SummonerMatchItemDto toDto(String puuid, String matchId, CachedMatch cached,
                                       Function<String, String> traitIconFn,
                                       Function<String, String> traitNameFn,
                                       Function<String, String> itemIconFn) {
        try {
            MatchDto matchDto = CACHE_MAPPER.readValue(cached.getMatchJson(), MatchDto.class);
            MatchDto.MatchInfoDto info = matchDto.getInfo();
            if (info == null || info.getParticipants() == null) return null;

            return info.getParticipants().stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .map(p -> SummonerMatchItemDto.from(MatchSummaryResponse.of(matchId, info, p), traitIconFn, traitNameFn, itemIconFn))
                    .orElse(null);
        } catch (Exception e) {
            logger.error("매치 역직렬화 실패: matchId={}", matchId, e);
            return null;
        }
    }

    private List<SummonerMatchItemDto> toSummonerMatchItemDtoList(String puuid, List<CachedMatch> matches,
                                                                   Function<String, String> traitIconFn,
                                                                   Function<String, String> traitNameFn,
                                                                   Function<String, String> itemIconFn) {
        return matches.stream()
                .map(m -> toDto(puuid, m.getMatchId(), m, traitIconFn, traitNameFn, itemIconFn))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void refreshMatches(String puuid) {
        List<String> matchIds = fetchMatchIds(puuid, 0, 20);
        if (matchIds.isEmpty()) return;

        Set<String> cachedIds = new HashSet<>(cachedMatchRepository.findMatchIdsByMatchIdIn(matchIds));
        List<String> toFetch = matchIds.stream()
                .filter(id -> !cachedIds.contains(id))
                .collect(Collectors.toList());

        if (!toFetch.isEmpty()) {
            collectInBackground(puuid, toFetch, toFetch.size());
        }
    }

    @Override
    public List<MatchSummaryResponse> getRankedMatchSummaries(String puuid, int count) {
        // DB에서 queueId=1100 조건으로 직접 조회 (메모리 필터링 불필요)
        List<CachedMatch> cached = cachedMatchRepository
                .findByParticipantPuuidAndQueueId(puuid, 1100, PageRequest.of(0, count));

        // 캐시가 부족하면 비동기로 수집 트리거만 하고 현재 캐시 데이터로 즉시 반환
        // putIfAbsent로 선점해 중복 트리거 방지 (작업 등록 전에 선점)
        if (cached.size() < count && inProgressMap.putIfAbsent(puuid, Boolean.TRUE) == null) {
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        List<String> matchIds = riotQueue
                                .submit(() -> riotApiClient.getMatchIds(puuid, count, 0, 1100))
                                .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        Set<String> cachedIds = new HashSet<>(
                                cachedMatchRepository.findMatchIdsByMatchIdIn(matchIds));
                        List<String> toFetch = matchIds.stream()
                                .filter(id -> !cachedIds.contains(id))
                                .collect(Collectors.toList());
                        if (!toFetch.isEmpty()) {
                            // requestedFast=0으로 latch 즉시 완료 — executor 스레드 점유 없이 fire-and-forget
                            collectInBackground(puuid, toFetch, 0);
                        } else {
                            inProgressMap.remove(puuid);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        inProgressMap.remove(puuid);
                        logger.warn("AI용 랭크 전적 수집 인터럽트: puuid={}", puuid);
                    } catch (Exception e) {
                        inProgressMap.remove(puuid);
                        logger.warn("AI용 랭크 전적 백그라운드 수집 실패: puuid={}", puuid);
                    }
                }, matchCollectionExecutor);
            } catch (Exception e) {
                // runAsync 등록 자체 실패(RejectedExecutionException 등) 시 선점 해제
                inProgressMap.remove(puuid);
                logger.warn("AI용 랭크 전적 수집 작업 등록 실패: puuid={}", puuid);
            }
        }

        return cached.stream()
                .map(m -> toMatchSummaryResponse(puuid, m.getMatchId(), m))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MatchSummaryResponse toMatchSummaryResponse(String puuid, String matchId, CachedMatch cached) {
        try {
            MatchDto matchDto = CACHE_MAPPER.readValue(cached.getMatchJson(), MatchDto.class);
            MatchDto.MatchInfoDto info = matchDto.getInfo();
            if (info == null || info.getParticipants() == null) return null;

            return info.getParticipants().stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .map(p -> MatchSummaryResponse.of(matchId, info, p))
                    .orElse(null);
        } catch (Exception e) {
            logger.error("AI용 매치 역직렬화 실패: matchId={}", matchId, e);
            return null;
        }
    }

}
