package com.tftgogo.domain.match.controller.docs;

import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin - Match", description = "매치 관리자 API")
public interface AdminMatchControllerDocs {

    @Operation(
            summary = "매치 캐시 통계 조회",
            description = "DB에 캐시된 매치 수, 큐 유형별 건수, 최신/최오래된 게임 일시를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats();

    @Operation(
            summary = "Rate Limit 통계 조회",
            description = "Riot API Rate Limiter의 현재 상태(요청 카운트, 윈도우 만료 시각 등)를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<RateLimitStatsResponse>> getRateLimitStats();
}
