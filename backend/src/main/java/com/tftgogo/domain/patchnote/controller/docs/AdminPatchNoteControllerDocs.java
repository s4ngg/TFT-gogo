package com.tftgogo.domain.patchnote.controller.docs;

import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin PatchNotes", description = "패치노트 관리자 API")
public interface AdminPatchNoteControllerDocs {

    @Operation(summary = "관리자 패치노트 목록 조회", description = "삭제되지 않은 패치노트를 관리자 정렬 기준으로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패")
    })
    ResponseEntity<ApiResponse<List<PatchNoteResponse>>> getPatchNotes();

    @Operation(summary = "패치노트 생성", description = "관리자 권한으로 패치노트를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패")
    })
    ResponseEntity<ApiResponse<PatchNoteResponse>> createPatchNote(
            @Valid @RequestBody AdminPatchNoteRequest request
    );

    @Operation(summary = "패치노트 수정", description = "관리자 권한으로 패치노트 기본 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "패치노트 없음")
    })
    ResponseEntity<ApiResponse<PatchNoteResponse>> updatePatchNote(
            @PathVariable("patchNoteId") Long patchNoteId,
            @Valid @RequestBody AdminPatchNoteRequest request
    );

    @Operation(summary = "패치노트 숨김 처리", description = "관리자 권한으로 패치노트를 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "패치노트 없음")
    })
    ResponseEntity<ApiResponse<Void>> deletePatchNote(
            @PathVariable("patchNoteId") Long patchNoteId
    );

    @Operation(summary = "패치 변경사항 생성", description = "관리자 권한으로 특정 패치노트의 변경사항을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "패치노트 없음")
    })
    ResponseEntity<ApiResponse<PatchChangeResponse>> createPatchChange(
            @Valid @RequestBody AdminPatchChangeRequest request
    );

    @Operation(summary = "패치 변경사항 수정", description = "관리자 권한으로 패치 변경사항을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "패치노트 또는 변경사항 없음")
    })
    ResponseEntity<ApiResponse<PatchChangeResponse>> updatePatchChange(
            @PathVariable("changeId") Long changeId,
            @Valid @RequestBody AdminPatchChangeRequest request
    );

    @Operation(summary = "패치 변경사항 숨김 처리", description = "관리자 권한으로 패치 변경사항을 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "변경사항 없음")
    })
    ResponseEntity<ApiResponse<Void>> deletePatchChange(
            @PathVariable("changeId") Long changeId
    );
}
