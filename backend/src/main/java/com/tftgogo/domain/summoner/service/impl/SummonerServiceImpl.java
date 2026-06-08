package com.tftgogo.domain.summoner.service.impl;

import com.tftgogo.domain.match.dto.response.RankInfoResponse;
import com.tftgogo.domain.match.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.summoner.service.SummonerService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.SummonerDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class SummonerServiceImpl implements SummonerService {

    private static final Logger logger = LogManager.getLogger(SummonerServiceImpl.class);
    private static final String PROFILE_ICON_URL_FORMAT =
            "https://ddragon.leagueoflegends.com/cdn/latest/img/profileicon/%d.png";

    private final RiotApiClient riotApiClient;

    public SummonerServiceImpl(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    @Override
    public SummonerProfileResponse getProfile(String gameName, String tagLine) {
        AccountDto account = riotApiClient.getAccount(gameName, tagLine);
        SummonerDto summoner = riotApiClient.getSummoner(account.getPuuid());
        String iconUrl = String.format(PROFILE_ICON_URL_FORMAT, summoner.getProfileIconId());
        return SummonerProfileResponse.of(account, summoner, iconUrl);
    }

    @Override
    public RankInfoResponse getRank(String puuid) {
        try {
            return riotApiClient.getLeagueByPuuid(puuid)
                    .map(RankInfoResponse::of)
                    .orElseGet(RankInfoResponse::unranked);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.RIOT_API_RATE_LIMIT
                    || e.getErrorCode() == ErrorCode.RIOT_API_ERROR) {
                throw e;
            }
            logger.warn("league 조회 실패, unranked 처리: puuid={}", puuid, e);
            return RankInfoResponse.unranked();
        }
    }

    @Override
    public SummonerDetailResponse getDetail(String gameName, String tagLine) {
        SummonerProfileResponse profile = getProfile(gameName, tagLine);
        RankInfoResponse rankInfo = getRank(profile.getPuuid());
        return SummonerDetailResponse.from(profile, rankInfo);
    }
}
