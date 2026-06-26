package com.tftgogo.domain.admin.service;

import com.tftgogo.domain.admin.entity.AdminAccount;
import com.tftgogo.domain.admin.entity.AdminRole;
import com.tftgogo.domain.admin.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private static final String BOOTSTRAP_USERNAME = "admin";

    @Value("${admin.bootstrap-password:}")
    private String bootstrapPassword;

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }

        boolean exists = adminAccountRepository.findByUsername(BOOTSTRAP_USERNAME).isPresent();
        if (exists) {
            log.info("[AdminBootstrap] '{}' 계정이 이미 존재합니다. 스킵합니다.", BOOTSTRAP_USERNAME);
            return;
        }

        try {
            AdminAccount master = AdminAccount.builder()
                    .username(BOOTSTRAP_USERNAME)
                    .password(passwordEncoder.encode(bootstrapPassword))
                    .role(AdminRole.MASTER)
                    .build();
            adminAccountRepository.save(master);
            log.info("[AdminBootstrap] MASTER 계정 '{}' 생성 완료.", BOOTSTRAP_USERNAME);
        } catch (DataIntegrityViolationException e) {
            // 동시 인스턴스가 먼저 생성 완료한 경우 — 정상
            log.info("[AdminBootstrap] '{}' 계정이 다른 인스턴스에 의해 이미 생성되었습니다.", BOOTSTRAP_USERNAME);
        }
    }
}
