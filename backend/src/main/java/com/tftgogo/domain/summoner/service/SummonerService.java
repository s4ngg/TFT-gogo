package com.tftgogo.domain.summoner.service;

import com.tftgogo.domain.match.dto.response.RankInfoResponse;
import com.tftgogo.domain.match.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;

public interface SummonerService {
    SummonerProfileResponse getProfile(String gameName, String tagLine);
    RankInfoResponse getRank(String puuid);
    SummonerDetailResponse getDetail(String gameName, String tagLine);
    SummonerDetailResponse refresh(String gameName, String tagLine);
}
