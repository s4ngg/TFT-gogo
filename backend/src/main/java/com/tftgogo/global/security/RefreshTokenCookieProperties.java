package com.tftgogo.global.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public class RefreshTokenCookieProperties {

    private String name = "refreshToken";

    private String path = "/api/v1/auth";

    private String sameSite = "Strict";

    private boolean secure = true;

    public void setName(String name) {
        validateNotBlank(name, "app.auth.refresh-cookie.name 설정은 필수입니다.");
        this.name = name;
    }

    public void setPath(String path) {
        validateNotBlank(path, "app.auth.refresh-cookie.path 설정은 필수입니다.");
        this.path = path;
    }

    public void setSameSite(String sameSite) {
        validateNotBlank(sameSite, "app.auth.refresh-cookie.same-site 설정은 필수입니다.");
        if (!isSameSiteAllowed(sameSite)) {
            throw new IllegalArgumentException("app.auth.refresh-cookie.same-site 설정은 Strict, Lax, None 중 하나여야 합니다.");
        }
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None refresh cookie는 secure=true 설정이 필요합니다.");
        }
        this.sameSite = sameSite;
    }

    public void setSecure(boolean secure) {
        if (!secure && "None".equalsIgnoreCase(sameSite)) {
            throw new IllegalArgumentException("SameSite=None refresh cookie는 secure=true 설정이 필요합니다.");
        }
        this.secure = secure;
    }

    private boolean isSameSiteAllowed(String candidate) {
        return "Strict".equalsIgnoreCase(candidate)
                || "Lax".equalsIgnoreCase(candidate)
                || "None".equalsIgnoreCase(candidate);
    }

    private void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
