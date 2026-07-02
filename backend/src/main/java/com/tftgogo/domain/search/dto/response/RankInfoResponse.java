package com.tftgogo.domain.search.dto.response;

import com.tftgogo.global.riot.dto.LeagueEntryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "랭크 정보 응답 (5.2 랭크 정보 영역, RANKED_TFT 큐타입 기준)")
public class RankInfoResponse {

    @Schema(description = "티어 (IRON ~ CHALLENGER). unranked=true 이면 null", example = "GOLD")
    private String tier;

    @Schema(description = "단계 (I ~ IV). 마스터 이상이거나 unranked=true 이면 null", example = "II")
    private String rank;

    @Schema(description = "현재 LP", example = "75")
    private int leaguePoints;

    @Schema(description = "Riot 서버 전체 누적 승리 수 (최근 30게임 승률 계산에는 사용하지 않음)", example = "134")
    private int wins;

    @Schema(description = "Riot 서버 전체 누적 패배 수 (최근 30게임 승률 계산에는 사용하지 않음)", example = "98")
    private int losses;

    @Schema(description = "배치 미완료 여부. true 이면 tier/rank/LP/wins/losses 는 의미 없음", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean unranked;

    public static RankInfoResponse of(LeagueEntryDto dto) {
        return RankInfoResponse.builder()
                .tier(dto.getTier())
                .rank(dto.getRank())
                .leaguePoints(dto.getLeaguePoints())
                .wins(dto.getWins())
                .losses(dto.getLosses())
                .unranked(false)
                .build();
    }

    public static RankInfoResponse unranked() {
        return RankInfoResponse.builder()
                .unranked(true)
                .build();
    }
}
