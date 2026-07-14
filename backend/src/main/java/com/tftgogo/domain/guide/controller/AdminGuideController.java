package com.tftgogo.domain.guide.controller;

import com.tftgogo.domain.content.scheduler.ContentRefreshImportGuard;
import com.tftgogo.domain.guide.controller.docs.AdminGuideControllerDocs;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/guides")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR')")
public class AdminGuideController implements AdminGuideControllerDocs {

    private final GuideCdragonImportService guideCdragonImportService;
    private final ContentRefreshImportGuard contentRefreshImportGuard;

    @PostMapping("/import/cdragon")
    public ResponseEntity<ApiResponse<GuideImportResponse>> importCdragonGuides(
            @RequestBody @Valid GuideCdragonImportRequest request) {
        GuideImportResponse response = contentRefreshImportGuard.runWithLock(
                "manual-guide-cdragon",
                () -> guideCdragonImportService.importGuides(request)
        );
        return ResponseEntity.ok(ApiResponse.success("Community Dragon 게임가이드 import 성공", response));
    }
}
