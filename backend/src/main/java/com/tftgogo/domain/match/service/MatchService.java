package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;

import java.util.List;

public interface MatchService {
    List<SummonerMatchItemDto> getMatches(String puuid, int start, int count);
    MatchDetailResponse getMatchDetail(String matchId);
    CollectionStatusResponse getCollectionStatus(String puuid);
}
