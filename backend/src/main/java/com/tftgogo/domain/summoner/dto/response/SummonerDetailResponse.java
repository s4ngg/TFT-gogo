package com.tftgogo.domain.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummonerDetailResponse {
    private String puuid;
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long summonerLevel;
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;
}
