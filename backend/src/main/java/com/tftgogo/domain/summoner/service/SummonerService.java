package com.tftgogo.domain.summoner.service;

import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;

import java.util.List;

public interface SummonerService {

    MatchSearchResponse search(String gameName, String tagLine);

    List<MatchSummaryResponse> getMatches(String puuid, int start);

    MatchDetailResponse getMatchDetail(String matchId);

    List<MatchSummaryResponse> getMatchesByRiotId(String gameName, String tagLine, int start);
}
