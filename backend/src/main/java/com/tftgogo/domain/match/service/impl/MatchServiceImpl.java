package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.match.service.MatchService;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.MatchDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger logger = LogManager.getLogger(MatchServiceImpl.class);
    private static final Set<Integer> VALID_QUEUE_IDS = Set.of(1090, 1100);

    private static final ObjectMapper CACHE_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    private final RiotApiClient riotApiClient;
    private final MatchCollectionService matchCollectionService;
    private final CachedMatchRepository cachedMatchRepository;

    public MatchServiceImpl(RiotApiClient riotApiClient,
                            MatchCollectionService matchCollectionService,
                            CachedMatchRepository cachedMatchRepository) {
        this.riotApiClient = riotApiClient;
        this.matchCollectionService = matchCollectionService;
        this.cachedMatchRepository = cachedMatchRepository;
    }

    @Override
    public List<SummonerMatchItemDto> getMatches(String puuid, int start, int count,
                                                  Function<String, String> traitIconFn,
                                                  Function<String, String> traitNameFn,
                                                  Function<String, String> itemIconFn) {
        return matchCollectionService.fetchAndCache(puuid, start, count, traitIconFn, traitNameFn, itemIconFn);
    }

    @Override
    public MatchDetailResponse getMatchDetail(String matchId) {
        return cachedMatchRepository.findById(matchId)
                .map(cached -> buildDetailFromCache(matchId, cached))
                .orElseGet(() -> fetchAndCacheDetail(matchId));
    }

    private MatchDetailResponse buildDetailFromCache(String matchId, CachedMatch cached) {
        try {
            MatchDto matchDto = CACHE_MAPPER.readValue(cached.getMatchJson(), MatchDto.class);
            MatchDto.MatchInfoDto info = matchDto.getInfo();
            if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) {
                throw new BusinessException(ErrorCode.RIOT_API_ERROR);
            }
            return MatchDetailResponse.of(matchId, matchDto);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("매치 캐시 역직렬화 실패, Riot API 재조회: matchId={}", matchId, e);
            return fetchAndCacheDetail(matchId);
        }
    }

    private MatchDetailResponse fetchAndCacheDetail(String matchId) {
        MatchDto match = riotApiClient.getMatch(matchId);
        MatchDto.MatchInfoDto info = match.getInfo();
        if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) {
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
        saveToCache(matchId, match);
        return MatchDetailResponse.of(matchId, match);
    }

    private void saveToCache(String matchId, MatchDto matchDto) {
        try {
            MatchDto.MatchInfoDto info = matchDto.getInfo();
            Set<String> puuids = info.getParticipants() == null ? Set.of() :
                    info.getParticipants().stream()
                            .map(MatchDto.ParticipantDto::getPuuid)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

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
            logger.warn("매치 상세 캐시 저장 실패: matchId={}", matchId, e);
        }
    }
}
