package com.tftgogo.domain.content.config;

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
@ConfigurationProperties(prefix = "app.content-refresh.monitoring")
public class ContentRefreshMonitoringProperties {

    @Positive
    private long staleAfterHours = 26;

    @Positive
    private int consecutiveFailureCriticalThreshold = 3;

    @Positive
    private long runningTimeoutMinutes = 120;
}
