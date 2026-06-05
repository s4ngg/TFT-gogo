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
    private List<TopTraitDto> topTraits;
    private List<TopChampionDto> topChampions;

    @Getter
    @Builder
    public static class TopTraitDto {
        private String traitId;
        private String name;
        private int count;
        private String iconUrl;
        private String tone;
        private int games;
        private double avgPlace;
    }

    @Getter
    @Builder
    public static class TopChampionDto {
        private String characterId;
        private String name;
        private String imageUrl;
        private int cost;
        private int games;
        private double avgPlace;
    }
}
