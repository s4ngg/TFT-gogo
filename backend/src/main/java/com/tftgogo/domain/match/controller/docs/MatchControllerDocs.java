package com.tftgogo.domain.match.controller.docs;

import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
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
            summary = "매치 목록 조회",
            description = "PUUID 기준으로 start 오프셋부터 count 건의 매치를 반환합니다. "
                    + "DB 캐시에 데이터가 없으면 Riot API에서 수집 후 첫 10개 완료 시 즉시 반환하고 나머지는 백그라운드 저장합니다. "
                    + "(queue_id 1090·1100만 포함)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<List<SummonerMatchItemDto>>> getMatches(
            @Parameter(description = "소환사 PUUID", required = true)
            @PathVariable("puuid") String puuid,
            @Parameter(description = "시작 인덱스 (0부터, count 단위 증가)", example = "0")
            @RequestParam(name = "start", defaultValue = "0") int start,
            @Parameter(description = "조회 건수 (기본값 20)", example = "20")
            @RequestParam(name = "count", defaultValue = "20") int count
    );

    @Operation(
            summary = "매치 상세 조회",
            description = "matchId 기준으로 해당 게임에 참가한 8인 전체 데이터를 반환합니다. DB 캐시 우선 조회."
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
