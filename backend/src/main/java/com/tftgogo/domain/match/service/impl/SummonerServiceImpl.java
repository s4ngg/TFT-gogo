package com.tftgogo.domain.match.service.impl;

import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.dto.response.RankInfoResponse;
import com.tftgogo.domain.match.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.match.service.SummonerService;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.LeagueEntryDto;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.dto.SummonerDto;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class SummonerServiceImpl implements SummonerService {

    private static final Logger logger = LogManager.getLogger(SummonerServiceImpl.class);

    private static final int MATCH_COUNT = 30;
    private static final Set<Integer> VALID_QUEUE_IDS = Set.of(1090, 1100);
    private static final String PROFILE_ICON_URL_FORMAT =
            "https://ddragon.leagueoflegends.com/cdn/latest/img/profileicon/%d.png";

    private final RiotApiClient riotApiClient;

    /**
     * gameName#tagLine 기준 소환사 전적 검색.
     *
     * @param gameName 소환사 게임 이름 (account-v1 gameName)
     * @param tagLine  소환사 태그라인 (account-v1 tagLine)
     * @return 소환사 프로필·랭크·최근 30 매치 통합 응답
     */
    @Override
    public MatchSearchResponse search(String gameName, String tagLine) {
        AccountDto account = riotApiClient.getAccount(gameName, tagLine);
        String puuid = account.getPuuid();

        // summoner-v1 · league-v1 병렬 호출 (스펙 2.1)
        CompletableFuture<SummonerDto> summonerFuture = CompletableFuture.supplyAsync(
                () -> riotApiClient.getSummoner(puuid));
        CompletableFuture<Optional<LeagueEntryDto>> leagueFuture = CompletableFuture
                .supplyAsync(() -> riotApiClient.getLeagueByPuuid(puuid))
                .exceptionally(e -> {
                    logger.warn("league 조회 실패, unranked 처리: puuid={}", puuid, e);
                    return Optional.empty();
                });

        try {
            CompletableFuture.allOf(summonerFuture, leagueFuture).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BusinessException be) throw be;
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
        SummonerDto summoner = summonerFuture.join();
        Optional<LeagueEntryDto> leagueEntry = leagueFuture.join();

        String iconUrl = String.format(PROFILE_ICON_URL_FORMAT, summoner.getProfileIconId());
        SummonerProfileResponse profile = SummonerProfileResponse.of(account, summoner, iconUrl);
        RankInfoResponse rankInfo = leagueEntry
                .map(RankInfoResponse::of)
                .orElseGet(RankInfoResponse::unranked);

        List<String> matchIds = riotApiClient.getMatchIds(puuid, MATCH_COUNT);
        List<MatchSummaryResponse> matchList = buildMatchList(puuid, matchIds);

        String recentWinRate = calculateWinRate(matchList);
        return MatchSearchResponse.of(profile, rankInfo, matchList, recentWinRate);
    }

    @Override
    public List<MatchSummaryResponse> getMatches(String puuid, int start) {
        List<String> matchIds = riotApiClient.getMatchIds(puuid, MATCH_COUNT, start);
        return buildMatchList(puuid, matchIds);
    }

    /**
     * 매치 ID 목록으로 매치 상세를 조회하여 소환사 기준 요약 목록을 구성.
     * queue_id가 VALID_QUEUE_IDS(1090·1100)에 없거나 조회 실패한 매치는 건너뜀.
     *
     * @param puuid    기준 소환사 PUUID
     * @param matchIds Riot API에서 받은 매치 ID 목록
     * @return 유효한 매치 요약 목록
     */
    private List<MatchSummaryResponse> buildMatchList(String puuid, List<String> matchIds) {
        List<MatchSummaryResponse> result = new ArrayList<>();
        for (String matchId : matchIds) {
            try {
                MatchDto match = riotApiClient.getMatch(matchId);
                MatchDto.MatchInfoDto info = match.getInfo();
                if (info == null || !VALID_QUEUE_IDS.contains(info.getQueue_id())) {
                    continue;
                }
                info.getParticipants().stream()
                        .filter(p -> puuid.equals(p.getPuuid()))
                        .findFirst()
                        .ifPresent(p -> result.add(MatchSummaryResponse.of(matchId, info, p)));
            } catch (Exception e) {
                logger.warn("매치 상세 조회 실패, 건너뜀: matchId={}", matchId, e);
            }
        }
        return result;
    }

    /**
     * placement 기준 승률 계산 (placement ≤ 4 = 승, > 4 = 패).
     *
     * @param matchList 매치 요약 목록
     * @return "66.7%" 형식 문자열, 매치가 없으면 null
     */
    private String calculateWinRate(List<MatchSummaryResponse> matchList) {
        if (matchList.isEmpty()) {
            return null;
        }
        long wins = matchList.stream().filter(m -> m.getPlacement() <= 4).count();
        double rate = (double) wins / matchList.size() * 100;
        return String.format("%.1f%%", rate);
    }
}
