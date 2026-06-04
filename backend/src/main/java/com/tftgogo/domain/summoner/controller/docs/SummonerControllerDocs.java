package com.tftgogo.domain.summoner.controller.docs;

import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItem;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Summoner", description = "소환사 전적 검색 API")
public interface SummonerControllerDocs {

    @Operation(
            summary = "소환사 상세 조회",
            description = "gameName#tagLine 기준으로 소환사 프로필과 랭크 정보(티어·LP·승패)를 반환합니다."
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

    @Operation(
            summary = "소환사 매치 목록 조회",
            description = "gameName#tagLine 기준으로 최근 30 매치 목록을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소환사를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Riot API Rate Limit 초과")
    })
    ResponseEntity<ApiResponse<List<SummonerMatchItem>>> getMatches(
            @Parameter(description = "소환사 게임 이름", example = "Hide on bush", required = true)
            @PathVariable("gameName") String gameName,
            @Parameter(description = "소환사 태그라인", example = "KR1", required = true)
            @PathVariable("tagLine") String tagLine
    );
}
