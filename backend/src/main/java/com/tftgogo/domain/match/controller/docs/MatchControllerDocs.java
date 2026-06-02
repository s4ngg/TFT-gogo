package com.tftgogo.domain.match.controller.docs;

import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Match", description = "TFT 전적 검색 API")
public interface MatchControllerDocs {

    @Operation(
            summary = "소환사 전적 검색",
            description = "gameName#tagLine 기준으로 소환사 프로필·랭크·최근 30 매치를 통합 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소환사를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<MatchSearchResponse>> search(
            @Parameter(description = "소환사 게임 이름", example = "Hide on bush", required = true)
            @RequestParam("gameName") String gameName,
            @Parameter(description = "소환사 태그라인", example = "KR1", required = true)
            @RequestParam("tagLine") String tagLine
    );

    @Operation(
            summary = "매치 목록 더보기",
            description = "PUUID 기준으로 start 오프셋부터 30건의 매치 요약을 추가 조회합니다. (queue_id 1090·1100만 포함)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> getMatches(
            @Parameter(description = "소환사 PUUID", required = true)
            @PathVariable("puuid") String puuid,
            @Parameter(description = "시작 인덱스 (0부터, 30 단위 증가)", example = "30")
            @RequestParam(name = "start", defaultValue = "0") int start
    );

    @Operation(
            summary = "매치 상세 조회",
            description = "matchId 기준으로 해당 게임에 참가한 8인 전체 데이터를 조회합니다. queue_id가 1090·1100이 아닌 경우 오류를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매치를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<MatchDetailResponse>> getMatchDetail(
            @Parameter(description = "Riot 매치 ID", example = "KR_7654321098", required = true)
            @PathVariable("matchId") String matchId
    );
}
