package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.AdminMatchControllerDocs;
import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;
import com.tftgogo.domain.match.service.AdminMatchService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/match")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR', 'ADMIN_VIEWER')")
public class AdminMatchController implements AdminMatchControllerDocs {

    private final AdminMatchService adminMatchService;

    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats() {
        return ResponseEntity.ok(ApiResponse.success("캐시 통계 조회 성공",
                adminMatchService.getCacheStats()));
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<ApiResponse<RateLimitStatsResponse>> getRateLimitStats() {
        return ResponseEntity.ok(ApiResponse.success("Rate Limit 조회 성공",
                adminMatchService.getRateLimitStats()));
    }
}
