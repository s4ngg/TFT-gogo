package com.tftgogo.domain.deck.controller.docs;

import com.tftgogo.domain.deck.dto.request.HeroAugmentDeckRequest;
import com.tftgogo.domain.deck.dto.response.HeroAugmentDeckResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin Hero Augment Deck", description = "관리자 영웅증강 덱 API")
@SecurityRequirement(name = "X-Admin-Token")
public interface AdminHeroAugmentDeckControllerDocs {

    @Operation(summary = "영웅증강 덱 목록 조회", description = "sortOrder 오름차순으로 전체 덱 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패")
    })
    ResponseEntity<ApiResponse<List<HeroAugmentDeckResponse>>> list();

    @Operation(summary = "영웅증강 덱 생성", description = "새 영웅증강 덱을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패")
    })
    ResponseEntity<ApiResponse<HeroAugmentDeckResponse>> create(@RequestBody @Valid HeroAugmentDeckRequest request);

    @Operation(summary = "영웅증강 덱 수정", description = "id에 해당하는 덱을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "덱을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<HeroAugmentDeckResponse>> update(@PathVariable Long id, @RequestBody @Valid HeroAugmentDeckRequest request);

    @Operation(summary = "영웅증강 덱 삭제", description = "id에 해당하는 덱을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "덱을 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id);
}
