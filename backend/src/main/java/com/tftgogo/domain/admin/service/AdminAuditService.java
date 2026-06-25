package com.tftgogo.domain.admin.service;

import com.tftgogo.domain.admin.entity.AdminAuditLog;
import com.tftgogo.domain.admin.repository.AdminAuditLogRepository;
import com.tftgogo.domain.admin.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> log(AdminPrincipal principal, String ip, String userAgent,
                                       String action, String target) {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminId(principal.getAdminId())
                .username(principal.getUsername())
                .ip(ip)
                .userAgent(userAgent != null && userAgent.length() > 500
                        ? userAgent.substring(0, 500) : userAgent)
                .action(action)
                .target(target)
                .build());
        return CompletableFuture.completedFuture(null);
    }
}
