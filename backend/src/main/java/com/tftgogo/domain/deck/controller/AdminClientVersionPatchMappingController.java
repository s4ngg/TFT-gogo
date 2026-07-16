package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.AdminClientVersionPatchMappingControllerDocs;
import com.tftgogo.domain.deck.dto.request.AdminClientVersionPatchMappingRequest;
import com.tftgogo.domain.deck.dto.response.ClientVersionPatchMappingResponse;
import com.tftgogo.domain.deck.service.AdminClientVersionPatchMappingService;
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
public class AdminClientVersionPatchMappingController implements AdminClientVersionPatchMappingControllerDocs {

    private final AdminClientVersionPatchMappingService adminClientVersionPatchMappingService;

    @Override
    @GetMapping("/client-version-patch-mappings")
    public ResponseEntity<ApiResponse<List<ClientVersionPatchMappingResponse>>> getMappings() {
        List<ClientVersionPatchMappingResponse> response = adminClientVersionPatchMappingService.getMappings();
        return ResponseEntity.ok(ApiResponse.success("클라이언트 버전 매핑 조회 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PostMapping("/client-version-patch-mappings")
    public ResponseEntity<ApiResponse<ClientVersionPatchMappingResponse>> createMapping(
            @Valid @RequestBody AdminClientVersionPatchMappingRequest request) {
        ClientVersionPatchMappingResponse response = adminClientVersionPatchMappingService.createMapping(request);
        return ResponseEntity.ok(ApiResponse.success("클라이언트 버전 매핑 생성 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @PatchMapping("/client-version-patch-mappings/{mappingId}")
    public ResponseEntity<ApiResponse<ClientVersionPatchMappingResponse>> updateMapping(
            @PathVariable("mappingId") Long mappingId,
            @Valid @RequestBody AdminClientVersionPatchMappingRequest request) {
        ClientVersionPatchMappingResponse response =
                adminClientVersionPatchMappingService.updateMapping(mappingId, request);
        return ResponseEntity.ok(ApiResponse.success("클라이언트 버전 매핑 수정 성공", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
    @DeleteMapping("/client-version-patch-mappings/{mappingId}")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable("mappingId") Long mappingId) {
        adminClientVersionPatchMappingService.deleteMapping(mappingId);
        return ResponseEntity.ok(ApiResponse.success("클라이언트 버전 매핑 삭제 성공", null));
    }
}
