package com.tftgogo.domain.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SummonerStatsDto {
    private List<TraitStatDto> topTraits;
    private List<ChampionStatDto> topChampions;

    @Getter
    @Builder
    public static class TraitStatDto {
        private String traitId;
        private String name;
        private String iconUrl;
        private String tone;
        private int count;
        private int games;
        private double avgPlace;
    }

    @Getter
    @Builder
    public static class ChampionStatDto {
        private String characterId;
        private String imageUrl;
        private int games;
        private double avgPlace;
    }
}
