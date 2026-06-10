package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
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
    public List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count) {
        // DB에 충분한 데이터가 있으면 즉시 반환
        List<CachedMatch> existing = cachedMatchRepository.findByParticipantPuuid(
                puuid, PageRequest.of(0, start + count));
        if (existing.size() >= start + count) {
            return toSummonerMatchItemDtoList(puuid, existing.subList(start, start + count));
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

        return buildResult(puuid, matchIds);
    }

    private List<String> fetchMatchIds(String puuid, int start, int count) {
        try {
            CompletableFuture<List<String>> rankedFuture =
                    riotQueue.submit(() -> riotApiClient.getMatchIdsForQueue(puuid, count, start, 1100));
            CompletableFuture<List<String>> normalFuture =
                    riotQueue.submit(() -> riotApiClient.getMatchIdsForQueue(puuid, count, start, 1090));

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
            riotQueue.submit(() -> riotApiClient.getMatchForQueue(matchId))
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

    private List<SummonerMatchItemDto> buildResult(String puuid, List<String> matchIds) {
        Map<String, CachedMatch> cacheMap = cachedMatchRepository.findAllById(matchIds)
                .stream()
                .collect(Collectors.toMap(CachedMatch::getMatchId, m -> m));

        return matchIds.stream()
                .filter(cacheMap::containsKey)
                .map(id -> toDto(puuid, id, cacheMap.get(id)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private SummonerMatchItemDto toDto(String puuid, String matchId, CachedMatch cached) {
        try {
            MatchDto matchDto = CACHE_MAPPER.readValue(cached.getMatchJson(), MatchDto.class);
            MatchDto.MatchInfoDto info = matchDto.getInfo();
            if (info == null || info.getParticipants() == null) return null;

            return info.getParticipants().stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .map(p -> SummonerMatchItemDto.from(MatchSummaryResponse.of(matchId, info, p)))
                    .orElse(null);
        } catch (Exception e) {
            logger.error("매치 역직렬화 실패: matchId={}", matchId, e);
            return null;
        }
    }

    private List<SummonerMatchItemDto> toSummonerMatchItemDtoList(String puuid, List<CachedMatch> matches) {
        return matches.stream()
                .map(m -> toDto(puuid, m.getMatchId(), m))
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
        // 캐시에서 랭크 게임(queueId=1100)만 조회
        List<CachedMatch> cached = cachedMatchRepository
                .findByParticipantPuuid(puuid, PageRequest.of(0, count * 2))  // 여유분 확보
                .stream()
                .filter(m -> m.getQueueId() == 1100)
                .limit(count)
                .collect(Collectors.toList());

        if (cached.size() < count) {
            // 부족하면 Riot API로 수집 시도 (동기 대기)
            try {
                List<String> matchIds = riotQueue
                        .submit(() -> riotApiClient.getMatchIdsForQueue(puuid, count, 0, 1100))
                        .get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                Set<String> cachedIds = new HashSet<>(
                        cachedMatchRepository.findMatchIdsByMatchIdIn(matchIds));
                List<String> toFetch = matchIds.stream()
                        .filter(id -> !cachedIds.contains(id))
                        .collect(Collectors.toList());

                if (!toFetch.isEmpty()) {
                    collectInBackground(puuid, toFetch, toFetch.size());
                }

                // 수집 후 재조회
                cached = cachedMatchRepository
                        .findByParticipantPuuid(puuid, PageRequest.of(0, count * 2))
                        .stream()
                        .filter(m -> m.getQueueId() == 1100)
                        .limit(count)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("AI용 랭크 전적 수집 실패, 캐시 데이터만 사용: puuid={}", puuid);
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

    @Override
    public CollectionStatusResponse getStatus(String puuid) {
        long collected = cachedMatchRepository.countByParticipantPuuid(puuid);
        boolean running = inProgressMap.getOrDefault(puuid, false);
        return CollectionStatusResponse.builder()
                .collected((int) collected)
                .inProgress(running)
                .build();
    }
}
