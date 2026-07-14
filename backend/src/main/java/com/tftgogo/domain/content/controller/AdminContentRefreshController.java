package com.tftgogo.domain.content.controller;

import com.tftgogo.domain.content.controller.docs.AdminContentRefreshControllerDocs;
import com.tftgogo.domain.content.dto.response.ContentRefreshHealthResponse;
import com.tftgogo.domain.content.service.ContentRefreshMonitoringService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-refresh")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR', 'ADMIN_VIEWER')")
public class AdminContentRefreshController implements AdminContentRefreshControllerDocs {

    private final ContentRefreshMonitoringService contentRefreshMonitoringService;

    @Override
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<ContentRefreshHealthResponse>> getHealth() {
        ContentRefreshHealthResponse response = contentRefreshMonitoringService.getHealth();
        return ResponseEntity.ok(ApiResponse.success("콘텐츠 수집 상태 조회 성공", response));
    }
}
