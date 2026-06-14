package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Riot API tft-match-v1 매치 상세 응답")
public class MatchDto {

    @Schema(description = "매치 상세 정보 (InfoDto)")
    private MatchInfoDto info;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "게임 메타 정보 및 참가자 목록")
    public static class MatchInfoDto {

        @Schema(description = "게임 시작 시각 (epoch milliseconds)", example = "1717200000000", requiredMode = Schema.RequiredMode.REQUIRED)
        private long game_datetime;

        @Schema(description = "게임 총 시간 (초)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
        private float game_length;

        @Schema(description = "패치 버전 문자열", example = "Version 14.12.639.9834 (Sep 10 2024/11:11:11) [PUBLIC] <Releases/14.12>", requiredMode = Schema.RequiredMode.REQUIRED)
        private String game_version;

        @Schema(description = "큐타입 ID (1090=일반, 1100=랭크)", example = "1100", requiredMode = Schema.RequiredMode.REQUIRED)
        private int queue_id;

        @Schema(description = "TFT 세트 번호", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tft_set_number;

        @Schema(description = "세트 내부 코어명", example = "TFTSet13", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tft_set_core_name;

        @Schema(description = "게임 타입", example = "standard", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tft_game_type;

        @Schema(description = "참가자 목록 (8명)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<ParticipantDto> participants;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "참가자 정보")
    public static class ParticipantDto {

        @Schema(description = "참가자 PUUID", example = "abcd1234-...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String puuid;

        @Schema(description = "참가자 게임 이름 (account-v1 gameName)", example = "Hide on bush", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdGameName;

        @Schema(description = "참가자 태그라인 (account-v1 tagLine)", example = "KR1", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdTagline;

        @Schema(description = "최종 순위 (1~8). 4 이하=승, 5 이상=패", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int placement;

        @Schema(description = "소환사 레벨", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
        private int level;

        @Schema(description = "마지막 라운드", example = "27", requiredMode = Schema.RequiredMode.REQUIRED)
        private int last_round;

        @Schema(description = "남은 골드", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int gold_left;

        @Schema(description = "처치한 플레이어 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int players_eliminated;

        @Schema(description = "탈락 시각 (초)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
        private float time_eliminated;

        @Schema(description = "플레이어에게 준 총 피해량", example = "48", requiredMode = Schema.RequiredMode.REQUIRED)
        private int total_damage_to_players;

        @Schema(description = "배치된 유닛 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<UnitDto> units;

        @Schema(description = "활성화된 특성 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<TraitDto> traits;

        @Schema(description = "선택한 증강체 ID 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private List<String> augments;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "배치된 유닛 정보")
    public static class UnitDto {

        @Schema(description = "유닛 내부 캐릭터 ID", example = "TFT13_Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String character_id;

        @Schema(description = "유닛 표시 이름", example = "Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "유닛 성급 (1~3)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tier;

        @Schema(description = "유닛 코스트 등급 (0=1코, 1=2코, 2=3코, 4=4코, 6=5코)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int rarity;

        @Schema(description = "장착 아이템 이름 배열", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> itemNames;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "활성화된 특성 정보")
    public static class TraitDto {

        @Schema(description = "특성 내부 키", example = "Set13_Challenger", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "배치된 해당 특성 유닛 수", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int num_units;

        @Schema(description = "활성화 단계 스타일 (0=비활성, 1=브론즈, 2=실버, 3=골드, 4=프리즈매틱)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int style;

        @Schema(description = "현재 활성화 단계", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tier_current;

        @Schema(description = "최대 활성화 단계", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tier_total;
    }
}
