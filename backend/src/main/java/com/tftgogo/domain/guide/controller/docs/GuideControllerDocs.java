package com.tftgogo.domain.guide.controller.docs;

import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Guide", description = "게임가이드 API")
public interface GuideControllerDocs {

    @Operation(summary = "게임가이드 대표 목록 조회", description = "공개 중인 게임가이드 데이터를 정렬 순서 기준으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<GuideEntryResponse>>> getGuideCatalog();

    @Operation(summary = "게임가이드 탭별 조회", description = "시너지, 아이템, 증강체, 챔피언 탭별 게임가이드 데이터를 페이지 단위로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<ApiResponse<GuidePageResponse<GuideEntryResponse>>> getGuideTabItems(
            @Parameter(description = "가이드 탭", example = "champions")
            @PathVariable("tab") String tab,
            @Parameter(description = "패치 버전", example = "17.3")
            @RequestParam(name = "patchVersion", required = false) String patchVersion,
            @Parameter(description = "검색어", example = "카이사")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(description = "페이지 번호", example = "1")
            @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @Parameter(description = "정렬 기준. 전용 테이블 응답에서는 현재 기본 정렬만 사용합니다.")
            @RequestParam(name = "sortKey", required = false) String sortKey,
            @Parameter(description = "정렬 방향(asc, desc)", example = "asc")
            @RequestParam(name = "sortDir", required = false) String sortDir,
            @Parameter(description = "챔피언 코스트 필터", example = "4")
            @RequestParam(name = "cost", required = false) Integer cost
    );
}
