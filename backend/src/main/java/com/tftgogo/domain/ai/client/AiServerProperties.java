package com.tftgogo.domain.ai.client;

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
@ConfigurationProperties(prefix = "ai.server")
public class AiServerProperties {

    @NotBlank(message = "ai.server.url 설정은 필수입니다.")
    private String url;

    @Positive(message = "ai.server.timeout-seconds는 0보다 커야 합니다.")
    private int timeoutSeconds = 10;

    @NotBlank(message = "ai.server.internal-secret 설정은 필수입니다.")
    private String internalSecret;
}
