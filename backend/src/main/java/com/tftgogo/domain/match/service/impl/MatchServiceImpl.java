package com.tftgogo.domain.match.service.impl;

import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.service.MatchService;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.MatchDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger logger = LogManager.getLogger(MatchServiceImpl.class);
    private static final Set<Integer> VALID_QUEUE_IDS = Set.of(1090, 1100);
    private static final int MAX_CONCURRENT_RIOT_CALLS = 10;
    private final Semaphore riotApiSemaphore = new Semaphore(MAX_CONCURRENT_RIOT_CALLS);

    private final RiotApiClient riotApiClient;
    private final Executor riotApiExecutor;

    public MatchServiceImpl(RiotApiClient riotApiClient,
                            @Qualifier("riotApiExecutor") Executor riotApiExecutor) {
        this.riotApiClient = riotApiClient;
        this.riotApiExecutor = riotApiExecutor;
    }

    @Override
    public List<SummonerMatchItemDto> getMatches(String puuid, int start, int count) {
        if (start < 0) throw new IllegalArgumentException("start는 0 이상이어야 합니다: " + start);
        if (count < 0) throw new IllegalArgumentException("count는 0 이상이어야 합니다: " + count);
        List<String> matchIds = riotApiClient.getMatchIds(puuid, count, start);
        return buildMatchList(puuid, matchIds).stream()
                .map(SummonerMatchItemDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public MatchDetailResponse getMatchDetail(String matchId) {
        MatchDto match = riotApiClient.getMatch(matchId);
        MatchDto.MatchInfoDto info = match.getInfo();
        if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) {
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
        return MatchDetailResponse.of(matchId, match);
    }

    private List<MatchSummaryResponse> buildMatchList(String puuid, List<String> matchIds) {
        List<CompletableFuture<Optional<MatchSummaryResponse>>> futures = matchIds.stream()
                .map(matchId -> CompletableFuture.supplyAsync(
                        () -> fetchMatchSummary(puuid, matchId), riotApiExecutor))
                .collect(Collectors.toList());

        List<MatchSummaryResponse> result = new ArrayList<>();
        for (CompletableFuture<Optional<MatchSummaryResponse>> future : futures) {
            try {
                future.join().ifPresent(result::add);
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof BusinessException
                        && ((BusinessException) cause).getErrorCode() == ErrorCode.RIOT_API_RATE_LIMIT) {
                    throw (BusinessException) cause;
                }
                logger.warn("매치 조회 실패, 건너뜀: {}", cause.getMessage());
            }
        }
        return result;
    }

    private Optional<MatchSummaryResponse> fetchMatchSummary(String puuid, String matchId) {
        try {
            riotApiSemaphore.acquire();
            MatchDto match;
            try {
                match = riotApiClient.getMatch(matchId);
            } finally {
                riotApiSemaphore.release();
            }
            MatchDto.MatchInfoDto info = match.getInfo();
            if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) return Optional.empty();
            List<MatchDto.ParticipantDto> participants = info.getParticipants();
            if (participants == null || participants.isEmpty()) return Optional.empty();
            return participants.stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .map(p -> MatchSummaryResponse.of(matchId, info, p));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("매치 상세 조회 인터럽트: matchId={}", matchId);
            return Optional.empty();
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.RIOT_API_RATE_LIMIT) throw e;
            logger.warn("매치 상세 조회 실패, 건너뜀: matchId={}", matchId, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("매치 상세 조회 실패, 건너뜀: matchId={}", matchId, e);
            return Optional.empty();
        }
    }
}
