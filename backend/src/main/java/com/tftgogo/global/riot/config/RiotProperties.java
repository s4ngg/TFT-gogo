package com.tftgogo.global.riot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "riot")
public class RiotProperties {

    @NotBlank(message = "riot.api-key 설정은 필수입니다.")
    private String apiKey;
    private String krBaseUrl = "https://kr.api.riotgames.com";
    private String asiaBaseUrl = "https://asia.api.riotgames.com";
    @Positive(message = "riot.connect-timeout-ms는 0보다 커야 합니다.")
    private int connectTimeoutMs = 10_000;
    @Positive(message = "riot.read-timeout-ms는 0보다 커야 합니다.")
    private int readTimeoutMs = 10_000;
    private int shortRateLimitMax = 20;
    private long shortRateLimitWindowMs = 1_000L;
    private int longRateLimitMax = 100;
    private long longRateLimitWindowMs = 120_000L;

    @Positive(message = "riot.queue-worker-concurrency는 0보다 커야 합니다.")
    private int queueWorkerConcurrency = 3;
    @Positive(message = "riot.max-foreground-streak는 0보다 커야 합니다.")
    private int maxForegroundStreak = 5;
    @Positive(message = "riot.foreground-task-ttl-ms는 0보다 커야 합니다.")
    private long foregroundTaskTtlMs = 60_000L;
    @Positive(message = "riot.background-task-ttl-ms는 0보다 커야 합니다.")
    private long backgroundTaskTtlMs = 300_000L;

    @Positive(message = "riot.match-fetch-timeout-seconds는 0보다 커야 합니다.")
    private long matchFetchTimeoutSeconds = 60L;
}
