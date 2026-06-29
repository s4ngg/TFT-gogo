package com.tftgogo.domain.member.controller;

import com.tftgogo.domain.member.controller.docs.AuthControllerDocs;
import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.domain.member.service.SocialLoginStartService;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.response.ApiResponse;
import com.tftgogo.global.security.RefreshTokenCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private static final Logger logger = LogManager.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final MemberService memberService;
    private final SocialLoginStartService socialLoginStartService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = memberService.signup(request);
        return withRefreshCookie(response, "회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = memberService.login(request);
        return withRefreshCookie(response, "로그인 성공");
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request) {
        AuthResponse response = memberService.refresh(
                refreshTokenCookieService.resolveRefreshToken(request).orElse(null)
        );

        return withRefreshCookie(response, "토큰 재발급 성공");
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request
    ) {
        try {
            memberService.logout(
                    userId,
                    resolveBearerToken(authorization),
                    refreshTokenCookieService.resolveRefreshToken(request).orElse(null)
            );
        } catch (RuntimeException e) {
            logger.warn("Logout cleanup failed. userId={}", userId, e);
            return ResponseEntity
                    .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearCookie().toString())
                    .body(ApiResponse.fail(
                            ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                            ErrorCode.INTERNAL_SERVER_ERROR.getStatus()
                    ));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearCookie().toString())
                .body(ApiResponse.success("로그아웃 성공"));
    }

    @Override
    @GetMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<SocialLoginStartResponse>> getSocialLoginStartUrl(
            @PathVariable("provider") String provider
    ) {
        String baseUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .toUriString();

        return ResponseEntity.ok(ApiResponse.success(
                "소셜 로그인 시작 URL 조회 성공",
                socialLoginStartService.getStartUrl(provider, baseUrl)
        ));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withRefreshCookie(AuthResponse response, String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createCookie(response.getRefreshToken()).toString())
                .body(ApiResponse.success(message, response));
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length());
    }
}
