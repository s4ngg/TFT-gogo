package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.domain.summoner.dto.response.SummonerStatsDto;

import java.util.List;
import java.util.function.Function;

public interface MatchService {
    List<SummonerMatchItemDto> getMatches(String puuid, int start, int count,
                                          Function<String, String> traitIconFn,
                                          Function<String, String> traitNameFn,
                                          Function<String, String> itemIconFn);
    SummonerStatsDto getStats(String puuid,
                              Function<String, String> traitIconFn,
                              Function<String, String> traitNameFn);
    MatchDetailResponse getMatchDetail(String matchId);
    CollectionStatusResponse getCollectionStatus(String puuid);
}
