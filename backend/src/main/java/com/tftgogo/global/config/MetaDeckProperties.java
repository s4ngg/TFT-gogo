package com.tftgogo.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.meta-deck")
public class MetaDeckProperties {

    /** 서버 시작 시 누락 집계 자동 실행 여부 (기본값: false, 명시적 opt-in 필요) */
    private boolean startupAggregate = false;
}
