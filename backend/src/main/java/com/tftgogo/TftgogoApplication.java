package com.tftgogo;

import com.tftgogo.domain.admin.config.AdminBootstrapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class TftgogoApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TftgogoApplication.class);
        // OSIV(open-in-view) 기본값(true)은 요청 스레드가 컨트롤러 처리 전체 동안(외부 HTTP
        // 호출 포함) DB 커넥션을 쥐고 있게 만들어 동시성 상황에서 HikariCP 풀을 조기에
        // 고갈시킨다 (#733에서 GET /api/ai/recommend 부하테스트 중 확인 — getProfile()
        // 단계가 쿼리 자체가 아니라 커넥션 획득 대기로 140~240ms 소요됨). 각 리포지토리
        // 호출이 자체 트랜잭션 안에서 커넥션을 즉시 반환하도록 명시적으로 끈다.
        // application*.yml은 개인 설정 파일이라 커밋되지 않으므로(.gitignore), 모든
        // 환경(local/docker/prod)에 일관되게 적용되도록 코드 레벨 기본값으로 설정한다.
        app.setDefaultProperties(Map.of("spring.jpa.open-in-view", "false"));
        app.run(args);
    }
}
