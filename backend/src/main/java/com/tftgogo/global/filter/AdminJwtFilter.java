package com.tftgogo.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.admin.security.AdminJwtTokenProvider;
import com.tftgogo.domain.admin.security.AdminPrincipal;
import com.tftgogo.global.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminJwtFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/admin";
    private static final String LOGIN_PATH = "/api/admin/auth/login";
    private static final String REFRESH_PATH = "/api/admin/auth/refresh";

    private final AdminJwtTokenProvider adminJwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // login/refresh don't require auth
        if (path.equals(LOGIN_PATH) || path.equals(REFRESH_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token == null || !adminJwtTokenProvider.validateToken(token)) {
            writeUnauthorized(response);
            return;
        }

        AdminPrincipal principal = new AdminPrincipal(
                adminJwtTokenProvider.getAdminId(token),
                adminJwtTokenProvider.getUsername(token),
                adminJwtTokenProvider.getRole(token)
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_" + principal.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail("관리자 인증이 필요합니다."))
        );
    }
}
