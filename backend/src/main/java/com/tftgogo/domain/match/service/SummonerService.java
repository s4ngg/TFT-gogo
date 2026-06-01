package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;

import java.util.List;

public interface SummonerService {

    MatchSearchResponse search(String gameName, String tagLine);

    List<MatchSummaryResponse> getMatches(String puuid, int start);
}
