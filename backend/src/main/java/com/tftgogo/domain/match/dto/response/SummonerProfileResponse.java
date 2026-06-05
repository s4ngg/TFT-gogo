package com.tftgogo.domain.match.dto.response;

import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.SummonerDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "소환사 프로필 응답 (5.1 소환사 프로필 영역)")
public class SummonerProfileResponse {

    @Schema(description = "소환사 PUUID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String puuid;

    @Schema(description = "프로필 아이콘 ID (Community Dragon URL 조합에 사용)", example = "4947", requiredMode = Schema.RequiredMode.REQUIRED)
    private int profileIconId;

    @Schema(description = "소환사 게임 이름 (account-v1 gameName)", example = "Hide on bush", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameName;

    @Schema(description = "소환사 태그라인 (account-v1 tagLine)", example = "KR1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tagLine;

    @Schema(description = "프로필 아이콘 이미지 URL (profileIconId → Data Dragon URL)", example = "https://ddragon.leagueoflegends.com/cdn/14.12.1/img/profileicon/4947.png", requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileIconUrl;

    @Schema(description = "소환사 레벨 (tft-summoner-v1 summonerLevel)", example = "312", requiredMode = Schema.RequiredMode.REQUIRED)
    private long summonerLevel;

    public static SummonerProfileResponse of(AccountDto accountDto, SummonerDto summonerDto, String profileIconUrl) {
        return SummonerProfileResponse.builder()
                .puuid(accountDto.getPuuid())
                .profileIconId(summonerDto.getProfileIconId())
                .gameName(accountDto.getGameName())
                .tagLine(accountDto.getTagLine())
                .profileIconUrl(profileIconUrl)
                .summonerLevel(summonerDto.getSummonerLevel())
                .build();
    }
}
