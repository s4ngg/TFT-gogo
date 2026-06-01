package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {

    private MatchInfoDto info;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchInfoDto {
        private String tft_set_core_name;
        private String game_version;
        private List<ParticipantDto> participants;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantDto {
        private String puuid;
        private int placement;
        private List<UnitDto> units;
        private List<TraitDto> traits;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnitDto {
        private String character_id;   // TFT13_Jinx 형식
        private int tier;              // 별 등급 (1~3)
        private int rarity;            // 코스트 등급 (0=1코, 1=2코, 2=3코, 4=4코, 6=5코)
        private List<String> itemNames;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraitDto {
        private String name;            // Set13_Challenger 형식
        private int num_units;
        private int style;              // 0=inactive, 1=bronze, 2=silver, 3=gold, 4=prismatic
        private int tier_current;
    }
}
