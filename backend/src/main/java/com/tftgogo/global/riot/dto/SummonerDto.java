package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Riot API tft-summoner-v1 소환사 응답 (SummonerDTO)")
public class SummonerDto {

    @Schema(description = "암호화된 PUUID (정확히 78자)", example = "abcd1234-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String puuid;

    @Schema(description = "소환사 아이콘 ID. Data Dragon 이미지 URL 조합에 사용", example = "4947", requiredMode = Schema.RequiredMode.REQUIRED)
    private int profileIconId;

    @Schema(description = "소환사 레벨", example = "312", requiredMode = Schema.RequiredMode.REQUIRED)
    private long summonerLevel;

    @Schema(description = "마지막 수정일 (epoch milliseconds)", example = "1717200000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private long revisionDate;
}
