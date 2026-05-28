package com.tftgogo.domain.deck.controller.docs;

import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Meta Deck", description = "메타 덱 API")
public interface MetaDeckControllerDocs {

    @Operation(summary = "메타 덱 목록 조회", description = "Riot API 집계 기반 현재 패치 메타 덱 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<MetaDeckResponse>>> getMetaDecks();

    @Operation(summary = "메타 덱 수동 집계", description = "Riot API에서 데이터를 즉시 수집·집계합니다. (관리자 전용)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "집계 완료")
    })
    ResponseEntity<ApiResponse<Void>> triggerAggregate();
}
