package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Riot API tft-league-v1 리그 엔트리 응답 (LeagueEntryDTO)")
public class LeagueEntryDto {

    @Schema(description = "리그 ID", example = "8a293f9e-c2c3-4f5b-a1e0-xxxxxxxx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String leagueId;

    @Schema(description = "소환사 PUUID", example = "abcd1234-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String puuid;

    @Schema(description = "큐 타입 (RANKED_TFT만 사용)", example = "RANKED_TFT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueType;

    @Schema(description = "티어 (IRON ~ CHALLENGER)", example = "DIAMOND", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tier;

    @Schema(description = "단계 (I ~ IV). 마스터 이상 해당 없음", example = "II", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rank;

    @Schema(description = "현재 LP", example = "75", requiredMode = Schema.RequiredMode.REQUIRED)
    private int leaguePoints;

    @Schema(description = "총 승리 수 (Riot 서버 전체 누적)", example = "312", requiredMode = Schema.RequiredMode.REQUIRED)
    private int wins;

    @Schema(description = "총 패배 수 (Riot 서버 전체 누적)", example = "289", requiredMode = Schema.RequiredMode.REQUIRED)
    private int losses;
}
