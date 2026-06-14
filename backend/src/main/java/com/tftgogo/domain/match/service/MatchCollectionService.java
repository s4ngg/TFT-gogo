package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;

import java.util.List;
import java.util.function.Function;

public interface MatchCollectionService {
    List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count,
                                             Function<String, String> traitIconFn,
                                             Function<String, String> itemIconFn);
    CollectionStatusResponse getStatus(String puuid);
    void refreshMatches(String puuid);

    /** AI 서버 요청용: 랭크 게임만 필터해 MatchSummaryResponse(전체 필드) 반환 */
    List<MatchSummaryResponse> getRankedMatchSummaries(String puuid, int count);
}
