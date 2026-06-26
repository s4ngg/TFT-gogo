package com.tftgogo.domain.patchnote.controller;

import com.tftgogo.domain.patchnote.controller.docs.AdminPatchNoteControllerDocs;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.service.AdminPatchNoteService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR', 'ADMIN_VIEWER')")
public class AdminPatchNoteController implements AdminPatchNoteControllerDocs {

    private final AdminPatchNoteService adminPatchNoteService;

    @Override
    @GetMapping("/patch-notes")
    public ResponseEntity<ApiResponse<List<PatchNoteResponse>>> getPatchNotes() {
        List<PatchNoteResponse> response = adminPatchNoteService.getPatchNotes();
        return ResponseEntity.ok(ApiResponse.success("관리자 패치노트 조회 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PostMapping("/patch-notes")
    public ResponseEntity<ApiResponse<PatchNoteResponse>> createPatchNote(
            @Valid @RequestBody AdminPatchNoteRequest request) {
        PatchNoteResponse response = adminPatchNoteService.createPatchNote(request);
        return ResponseEntity.ok(ApiResponse.success("패치노트 생성 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PostMapping("/patch-notes/import/riot")
    public ResponseEntity<ApiResponse<AdminPatchNoteImportResponse>> importRiotPatchNote(
            @Valid @RequestBody(required = false) AdminPatchNoteImportRequest request) {
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);
        return ResponseEntity.ok(ApiResponse.success("Riot patch note import success", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PatchMapping("/patch-notes/{patchNoteId}")
    public ResponseEntity<ApiResponse<PatchNoteResponse>> updatePatchNote(
            @PathVariable("patchNoteId") Long patchNoteId,
            @Valid @RequestBody AdminPatchNoteRequest request) {
        PatchNoteResponse response = adminPatchNoteService.updatePatchNote(patchNoteId, request);
        return ResponseEntity.ok(ApiResponse.success("패치노트 수정 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @DeleteMapping("/patch-notes/{patchNoteId}")
    public ResponseEntity<ApiResponse<Void>> deletePatchNote(@PathVariable("patchNoteId") Long patchNoteId) {
        adminPatchNoteService.deletePatchNote(patchNoteId);
        return ResponseEntity.ok(ApiResponse.success("패치노트 숨김 처리 성공", null));
    }

    @Override
    @GetMapping("/patch-notes/{patchNoteId}/changes")
    public ResponseEntity<ApiResponse<List<PatchChangeResponse>>> getPatchChanges(
            @PathVariable("patchNoteId") Long patchNoteId) {
        List<PatchChangeResponse> response = adminPatchNoteService.getPatchChanges(patchNoteId);
        return ResponseEntity.ok(ApiResponse.success("패치 변경사항 조회 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PostMapping("/patch-note-changes")
    public ResponseEntity<ApiResponse<PatchChangeResponse>> createPatchChange(
            @Valid @RequestBody AdminPatchChangeRequest request) {
        PatchChangeResponse response = adminPatchNoteService.createPatchChange(request);
        return ResponseEntity.ok(ApiResponse.success("패치 변경사항 생성 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PatchMapping("/patch-note-changes/{changeId}")
    public ResponseEntity<ApiResponse<PatchChangeResponse>> updatePatchChange(
            @PathVariable("changeId") Long changeId,
            @Valid @RequestBody AdminPatchChangeRequest request) {
        PatchChangeResponse response = adminPatchNoteService.updatePatchChange(changeId, request);
        return ResponseEntity.ok(ApiResponse.success("패치 변경사항 수정 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @DeleteMapping("/patch-note-changes/{changeId}")
    public ResponseEntity<ApiResponse<Void>> deletePatchChange(@PathVariable("changeId") Long changeId) {
        adminPatchNoteService.deletePatchChange(changeId);
        return ResponseEntity.ok(ApiResponse.success("패치 변경사항 숨김 처리 성공", null));
    }
}
