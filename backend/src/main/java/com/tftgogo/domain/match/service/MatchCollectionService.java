package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.domain.summoner.dto.response.SummonerStatsDto;

import java.util.List;
import java.util.function.Function;

public interface MatchCollectionService {
    List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count,
                                             Function<String, String> traitIconFn,
                                             Function<String, String> traitNameFn,
                                             Function<String, String> itemIconFn);
    CollectionStatusResponse getStatus(String puuid);
    void refreshMatches(String puuid);

    /** AI 서버 요청용: 랭크 게임만 필터해 MatchSummaryResponse(전체 필드) 반환 */
    List<MatchSummaryResponse> getRankedMatchSummaries(String puuid, int count);

    /** 저장된 전체 게임 기준으로 시너지/챔피언 통계 집계 */
    SummonerStatsDto getAllMatchStats(String puuid,
                                     Function<String, String> traitIconFn,
                                     Function<String, String> traitNameFn);
}
