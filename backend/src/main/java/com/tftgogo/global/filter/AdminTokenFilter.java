package com.tftgogo.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 관리자 API(/api/admin/**)에 대한 토큰 기반 인증 필터.
 * X-Admin-Token 헤더 값이 설정된 토큰과 일치해야 접근 가능.
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String ADMIN_PATH_PREFIX = "/api/admin";

    @Value("${admin.secret-token}")
    private String adminSecretToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(ADMIN_TOKEN_HEADER);
        if (token == null || !token.equals(adminSecretToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"관리자 인증이 필요합니다.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
