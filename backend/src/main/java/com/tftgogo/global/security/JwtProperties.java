package com.tftgogo.global.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "jwt.secret 설정은 필수입니다.")
    @Size(min = 32, message = "jwt.secret은 32자 이상이어야 합니다.")
    private String secret;

    @Positive(message = "jwt.access-token-expiration-millis는 0보다 커야 합니다.")
    private long accessTokenExpirationMillis = 3_600_000L;
}
