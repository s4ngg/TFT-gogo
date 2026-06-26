package com.tftgogo.domain.admin.service;

import com.tftgogo.domain.admin.config.AdminBootstrapProperties;
import com.tftgogo.domain.admin.entity.AdminAccount;
import com.tftgogo.domain.admin.entity.AdminRole;
import com.tftgogo.domain.admin.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final AdminBootstrapProperties props;
    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (props.password() == null || props.password().isBlank()) {
            return;
        }

        boolean exists = adminAccountRepository.findByUsername(props.username()).isPresent();
        if (exists) {
            log.info("[AdminBootstrap] '{}' 계정이 이미 존재합니다. 스킵합니다.", props.username());
            return;
        }

        try {
            AdminAccount master = AdminAccount.builder()
                    .username(props.username())
                    .password(passwordEncoder.encode(props.password()))
                    .role(AdminRole.MASTER)
                    .build();
            adminAccountRepository.save(master);
            log.info("[AdminBootstrap] MASTER 계정 '{}' 생성 완료.", props.username());
        } catch (DataIntegrityViolationException e) {
            // 동시 인스턴스가 먼저 생성 완료한 경우 — 정상
            log.info("[AdminBootstrap] '{}' 계정이 다른 인스턴스에 의해 이미 생성되었습니다.", props.username());
        }
    }
}
