package com.tftgogo.global.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@Getter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "jwt.secret 설정은 필수입니다.")
    @Size(min = 32, message = "jwt.secret은 32자 이상이어야 합니다.")
    private String secret;

    private long accessTokenExpirationMillis = 3_600_000L;

    private long refreshTokenExpirationMillis = 1_209_600_000L;

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setAccessTokenExpirationMillis(long accessTokenExpirationMillis) {
        validatePositive(accessTokenExpirationMillis, "jwt.access-token-expiration-millis는 0보다 커야 합니다.");
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
    }

    public void setRefreshTokenExpirationMillis(long refreshTokenExpirationMillis) {
        validatePositive(refreshTokenExpirationMillis, "jwt.refresh-token-expiration-millis는 0보다 커야 합니다.");
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    private void validatePositive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
