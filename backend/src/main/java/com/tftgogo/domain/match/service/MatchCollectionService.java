package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.search.dto.response.SummonerMatchItemDto;

import java.util.List;
import java.util.function.Function;

public interface MatchCollectionService {
    List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count,
                                             Function<String, String> traitIconFn,
                                             Function<String, String> traitNameFn,
                                             Function<String, String> itemIconFn);
    /** maxWaitMs: 호출측(refresh() 전체 데드라인)이 남겨준 잔여 예산 — 내부 조회/수집 대기시간의 상한으로 사용 */
    void refreshMatches(String puuid, long maxWaitMs);

    /** AI 서버 요청용: 랭크 게임만 필터해 MatchSummaryResponse(전체 필드) 반환 */
    List<MatchSummaryResponse> getRankedMatchSummaries(String puuid, int count);
}
