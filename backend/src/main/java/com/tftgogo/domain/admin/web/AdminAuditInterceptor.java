package com.tftgogo.domain.admin.web;

import com.tftgogo.domain.admin.security.AdminPrincipal;
import com.tftgogo.domain.admin.service.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {

    private final AdminAuditService adminAuditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 실패한 요청(4xx/5xx)은 감사 로그 기록 안 함
        if (response.getStatus() >= 400) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminPrincipal principal)) {
            return;
        }

        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }

        // 요청 수명이 끝나기 전에 값을 스냅샷해서 비동기 스레드에 전달
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();
        String action = method + " " + uri;
        String target = resolveTarget(uri);

        adminAuditService.log(principal, ip, userAgent, action, target);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // /api/admin/decks/123/curation → "decks/123"
    // /api/admin/patch-notes/45 → "patch-notes/45"
    private String resolveTarget(String uri) {
        // /api/admin/<resource>[/<id>[/...]] 에서 resource/id 부분만 추출
        String prefix = "/api/admin/";
        if (!uri.startsWith(prefix)) return uri;
        String[] parts = uri.substring(prefix.length()).split("/");
        if (parts.length == 0 || parts[0].isBlank()) return uri;
        // resource + id(숫자)만 포함
        StringBuilder sb = new StringBuilder(parts[0]);
        if (parts.length > 1 && parts[1].matches("\\d+")) {
            sb.append('/').append(parts[1]);
        }
        return sb.toString();
    }
}
