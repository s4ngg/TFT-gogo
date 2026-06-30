package com.tftgogo.domain.admin.controller;

import com.tftgogo.domain.admin.dto.request.AdminLoginRequest;
import com.tftgogo.domain.admin.dto.response.AdminLoginResponse;
import com.tftgogo.domain.admin.dto.response.AdminTokenRefreshResponse;
import com.tftgogo.domain.admin.service.AdminAuthService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AdminLoginResponse response = adminAuthService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdminTokenRefreshResponse>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AdminTokenRefreshResponse response = adminAuthService.refresh(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        adminAuthService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }
}
