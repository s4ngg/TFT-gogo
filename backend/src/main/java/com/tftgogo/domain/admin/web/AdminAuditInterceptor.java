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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AdminPrincipal principal)) {
            return true;
        }

        String method = request.getMethod();
        // GET/HEAD/OPTIONS은 감사 로그 생략 (조회 전용)
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }

        String action = method + " " + request.getRequestURI();
        adminAuditService.log(principal, request, action, null);
        return true;
    }
}
