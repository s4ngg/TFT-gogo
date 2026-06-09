package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;

import java.util.List;

public interface MatchCollectionService {
    List<SummonerMatchItemDto> fetchAndCache(String puuid, int start, int count);
    CollectionStatusResponse getStatus(String puuid);
}
