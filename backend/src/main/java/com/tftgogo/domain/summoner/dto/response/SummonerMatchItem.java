package com.tftgogo.domain.summoner.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SummonerMatchItem {
    private String matchId;
    private int placement;
    private long gameDateTime;
    private String gameType;
    private String compositionName;
    private List<TraitDto> traits;
    private List<UnitDto> units;
    private List<ParticipantDto> participants;

    @Getter
    @Builder
    public static class TraitDto {
        private String traitId;
        private String name;
        private String iconUrl;
        private int count;
        private String tone;
    }

    @Getter
    @Builder
    public static class UnitDto {
        private String characterId;
        private String imageUrl;
        private int stars;
        private List<String> itemImageUrls;
    }

    @Getter
    @Builder
    public static class ParticipantDto {
        private String puuid;
        private String riotIdGameName;
        private String riotIdTagline;
        private int placement;
        private String stage;
        private List<TraitDto> traits;
        private List<UnitDto> units;
        private int playersEliminated;
        private int goldLeft;
    }
}
