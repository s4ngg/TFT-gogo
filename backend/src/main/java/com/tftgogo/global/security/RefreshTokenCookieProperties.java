package com.tftgogo.global.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public class RefreshTokenCookieProperties {

    @NotBlank(message = "app.auth.refresh-cookie.name 설정은 필수입니다.")
    private String name = "refreshToken";

    @NotBlank(message = "app.auth.refresh-cookie.path 설정은 필수입니다.")
    private String path = "/api/v1/auth";

    @NotBlank(message = "app.auth.refresh-cookie.same-site 설정은 필수입니다.")
    private String sameSite = "Strict";

    private boolean secure = true;
}
