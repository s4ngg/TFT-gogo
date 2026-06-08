package com.tftgogo.domain.summoner.controller.docs;

import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Summoner", description = "소환사 정보 API")
public interface SummonerControllerDocs {

    @Operation(
            summary = "소환사 정보 조회",
            description = "gameName#tagLine 기준으로 소환사 프로필과 랭크 정보를 반환합니다. 매치 데이터는 포함되지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소환사를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<SummonerDetailResponse>> getSummoner(
            @Parameter(description = "소환사 게임 이름", example = "Hide on bush", required = true)
            @PathVariable("gameName") String gameName,
            @Parameter(description = "소환사 태그라인", example = "KR1", required = true)
            @PathVariable("tagLine") String tagLine
    );
}
