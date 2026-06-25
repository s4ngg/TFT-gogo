package com.tftgogo.domain.admin.service;

import com.tftgogo.domain.admin.dto.request.AdminLoginRequest;
import com.tftgogo.domain.admin.dto.response.AdminLoginResponse;
import com.tftgogo.domain.admin.dto.response.AdminTokenRefreshResponse;
import com.tftgogo.domain.admin.entity.AdminAccount;
import com.tftgogo.domain.admin.entity.AdminAuditLog;
import com.tftgogo.domain.admin.entity.AdminRefreshToken;
import com.tftgogo.domain.admin.repository.AdminAccountRepository;
import com.tftgogo.domain.admin.repository.AdminAuditLogRepository;
import com.tftgogo.domain.admin.repository.AdminRefreshTokenRepository;
import com.tftgogo.domain.admin.security.AdminJwtTokenProvider;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String REFRESH_TOKEN_COOKIE = "admin_refresh";
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        AdminAccount admin = adminAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS));

        if (!admin.isEnabled()) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
        }

        String accessToken = adminJwtTokenProvider.createAccessToken(admin.getId(), admin.getUsername(), admin.getRole());
        String rawRefreshToken = issueRefreshToken(admin.getId());
        setRefreshCookie(httpResponse, rawRefreshToken);

        saveAuditLog(admin, httpRequest, "LOGIN", null);


        return new AdminLoginResponse(accessToken, admin.getUsername(), admin.getRole());
    }

    @Transactional
    public AdminTokenRefreshResponse refresh(HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        String raw = extractRefreshCookieValue(httpRequest);
        if (raw == null) {
            throw new BusinessException(ErrorCode.ADMIN_REFRESH_TOKEN_INVALID);
        }

        String hash = hashToken(raw);
        // 비관적 락으로 동시 rotation 분기 방지
        AdminRefreshToken stored = adminRefreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_REFRESH_TOKEN_INVALID));

        if (stored.isExpired()) {
            adminRefreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.ADMIN_REFRESH_TOKEN_EXPIRED);
        }

        AdminAccount admin = adminAccountRepository.findById(stored.getAdminAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (!admin.isEnabled()) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        // rotate refresh token
        adminRefreshTokenRepository.delete(stored);
        String newRaw = issueRefreshToken(admin.getId());
        setRefreshCookie(httpResponse, newRaw);

        String accessToken = adminJwtTokenProvider.createAccessToken(admin.getId(), admin.getUsername(), admin.getRole());
        return new AdminTokenRefreshResponse(accessToken);
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String raw = extractRefreshCookieValue(httpRequest);
        if (raw != null) {
            String hash = hashToken(raw);
            adminRefreshTokenRepository.findByTokenHashForUpdate(hash)
                    .ifPresent(adminRefreshTokenRepository::delete);
        }
        clearRefreshCookie(httpResponse);
    }

    private String issueRefreshToken(Long adminAccountId) {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AdminRefreshToken token = AdminRefreshToken.builder()
                .adminAccountId(adminAccountId)
                .tokenHash(hashToken(raw))
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .build();
        adminRefreshTokenRepository.save(token);
        return raw;
    }

    private String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/admin/auth")
                .maxAge(Duration.ofDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/admin/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void saveAuditLog(AdminAccount admin, HttpServletRequest request,
                               String action, String target) {
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .ip(ip)
                .userAgent(userAgent != null && userAgent.length() > 500
                        ? userAgent.substring(0, 500) : userAgent)
                .action(action)
                .target(target)
                .build());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
