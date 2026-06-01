package com.tftgogo.domain.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "소환사 전적 검색 통합 응답 (프로필 + 랭크 + 매치 목록 첫 30건). 더보기 클릭 시 /matches 엔드포인트로 start 오프셋을 증가시켜 30건씩 추가 요청")
public class MatchSearchResponse {

    @Schema(description = "소환사 프로필 (5.1 소환사 프로필 영역)", requiredMode = Schema.RequiredMode.REQUIRED)
    private SummonerProfileResponse profile;

    @Schema(description = "랭크 정보 (5.2 랭크 정보 영역, RANKED_TFT 큐타입 기준). 배치 미완료 시 unranked=true", requiredMode = Schema.RequiredMode.REQUIRED)
    private RankInfoResponse rankInfo;

    @Schema(description = "매치 목록 (queue_id 1090·1100만 포함, 30건 단위 페이지). 더보기 추가 요청 시에는 이 필드만 포함한 별도 응답 사용", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MatchSummaryResponse> matchList;

    @Schema(description = "현재 matchList 기준 승률 — placement<=4 승, >4 패로 계산. 게임 수가 0이면 null", example = "66.7%")
    private String recentWinRate;

    public static MatchSearchResponse of(SummonerProfileResponse profile, RankInfoResponse rankInfo,
                                         List<MatchSummaryResponse> matchList, String recentWinRate) {
        return MatchSearchResponse.builder()
                .profile(profile)
                .rankInfo(rankInfo)
                .matchList(matchList)
                .recentWinRate(recentWinRate)
                .build();
    }
}
