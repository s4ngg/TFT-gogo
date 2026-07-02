package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.search.dto.response.SummonerMatchItemDto;

import java.util.List;
import java.util.function.Function;

public interface MatchService {
    List<SummonerMatchItemDto> getMatches(String puuid, int start, int count,
                                          Function<String, String> traitIconFn,
                                          Function<String, String> traitNameFn,
                                          Function<String, String> itemIconFn);
    MatchDetailResponse getMatchDetail(String matchId);
}
