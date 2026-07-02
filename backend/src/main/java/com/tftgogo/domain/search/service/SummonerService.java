package com.tftgogo.domain.search.service;

import com.tftgogo.domain.search.dto.response.RankInfoResponse;
import com.tftgogo.domain.search.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.search.dto.response.SummonerProfileResponse;

public interface SummonerService {
    SummonerProfileResponse getProfile(String gameName, String tagLine);
    RankInfoResponse getRank(String puuid);
    SummonerDetailResponse getDetail(String gameName, String tagLine);
    SummonerDetailResponse refresh(String gameName, String tagLine);
}
