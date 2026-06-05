package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Riot API account-v1 계정 응답 (AccountDto)")
public class AccountDto {

    @Schema(description = "암호화된 PUUID. 이후 모든 API 호출의 기준 식별자", example = "abcd1234-...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String puuid;

    @Schema(description = "소환사 게임 이름. 응답에 없을 수 있음 (선택 필드)", example = "Hide on bush")
    private String gameName;

    @Schema(description = "소환사 태그라인. 응답에 없을 수 있음 (선택 필드)", example = "KR1")
    private String tagLine;
}
