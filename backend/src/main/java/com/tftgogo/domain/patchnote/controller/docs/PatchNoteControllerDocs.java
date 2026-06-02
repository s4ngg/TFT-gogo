package com.tftgogo.domain.patchnote.controller.docs;

import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "PatchNotes", description = "패치노트 API")
public interface PatchNoteControllerDocs {

    @Operation(summary = "패치노트 목록 조회", description = "공개 중인 패치노트를 현재 패치 우선, 적용일 최신순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<PatchNoteResponse>>> getPatchNotes();

    @Operation(summary = "패치 변경사항 조회", description = "특정 패치 버전의 변경사항을 필터링/페이지네이션하고 전체 변경사항 통계를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "패치노트 없음")
    })
    ResponseEntity<ApiResponse<PatchChangePageResponse>> getPatchChanges(
            @Parameter(description = "패치 버전", example = "17.0")
            @PathVariable String version,
            @Parameter(description = "변경 카테고리(CHAMPION, TRAIT, ITEM, AUGMENT, SYSTEM)", example = "CHAMPION")
            @RequestParam(required = false) String category,
            @Parameter(description = "변경 유형(BUFF, NERF, ADJUST, NEW)", example = "BUFF")
            @RequestParam(required = false) String type,
            @Parameter(description = "영향도(HIGH, MEDIUM, LOW)", example = "HIGH")
            @RequestParam(required = false) String impact,
            @Parameter(description = "검색어", example = "카이사")
            @RequestParam(required = false) String query,
            @Parameter(description = "페이지 번호", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(required = false) Integer pageSize
    );
}
