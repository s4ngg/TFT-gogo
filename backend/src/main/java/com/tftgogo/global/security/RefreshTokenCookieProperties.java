package com.tftgogo.global.security;

import jakarta.validation.constraints.AssertTrue;
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

    @AssertTrue(message = "app.auth.refresh-cookie.same-site 설정은 Strict, Lax, None 중 하나여야 합니다.")
    public boolean isSameSiteAllowed() {
        return "Strict".equalsIgnoreCase(sameSite)
                || "Lax".equalsIgnoreCase(sameSite)
                || "None".equalsIgnoreCase(sameSite);
    }

    @AssertTrue(message = "SameSite=None refresh cookie는 secure=true 설정이 필요합니다.")
    public boolean isSameSiteNoneSecure() {
        return !"None".equalsIgnoreCase(sameSite) || secure;
    }
}
