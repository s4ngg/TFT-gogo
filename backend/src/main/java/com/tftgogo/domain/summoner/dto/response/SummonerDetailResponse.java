package com.tftgogo.domain.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private double avgPlace;
    private double top4Rate;
    private int[] rankDistribution;
    private List<Object> topTraits;
    private List<Object> topChampions;
}
