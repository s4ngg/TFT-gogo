package com.tftgogo.domain.deck.controller.docs;

import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Meta Deck", description = "메타 덱 API")
public interface MetaDeckControllerDocs {

    @Operation(summary = "메타 덱 목록 조회", description = "랭크 구간별 메타 덱 목록을 반환합니다. (EMERALD_PLUS / DIAMOND_PLUS / MASTER_PLUS)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<MetaDeckListResponse>> getMetaDecks(
            @Parameter(description = "랭크 구간 필터", example = "EMERALD_PLUS")
            @RequestParam(name = "rankFilter", defaultValue = "EMERALD_PLUS") RankFilter rankFilter);

}
