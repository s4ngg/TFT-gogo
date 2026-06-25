package com.tftgogo.domain.admin.service;

import com.tftgogo.domain.admin.entity.AdminAuditLog;
import com.tftgogo.domain.admin.repository.AdminAuditLogRepository;
import com.tftgogo.domain.admin.security.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AdminPrincipal principal, HttpServletRequest request,
                    String action, String target) {
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminId(principal.getAdminId())
                .username(principal.getUsername())
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
