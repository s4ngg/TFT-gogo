package com.tftgogo.domain.patchnote.controller;

import com.tftgogo.domain.patchnote.controller.docs.PatchNoteControllerDocs;
import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.service.PatchNoteService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patch-notes")
@RequiredArgsConstructor
public class PatchNoteController implements PatchNoteControllerDocs {

    private final PatchNoteService patchNoteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatchNoteResponse>>> getPatchNotes() {
        List<PatchNoteResponse> response = patchNoteService.getPatchNotes();
        return ResponseEntity.ok(ApiResponse.success("패치노트 조회 성공", response));
    }

    @GetMapping("/{version}/changes")
    public ResponseEntity<ApiResponse<PatchChangePageResponse>> getPatchChanges(
            @PathVariable("version") String version,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "impact", required = false) String impact,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        PatchChangePageResponse response = patchNoteService.getPatchChanges(
                version,
                category,
                type,
                impact,
                query,
                page,
                pageSize
        );
        return ResponseEntity.ok(ApiResponse.success("패치 변경사항 조회 성공", response));
    }
}
