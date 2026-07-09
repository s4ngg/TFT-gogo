package com.tftgogo.domain.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "admin.bootstrap")
public record AdminBootstrapProperties(
        String username,
        String password
) {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Set<String> FORBIDDEN_PASSWORD_PARTS = Set.of(
            "admin",
            "password",
            "changeme",
            "qwerty",
            "123456",
            "tftgogo"
    );

    public AdminBootstrapProperties {
        if (username == null || username.isBlank()) {
            username = "admin";
        } else {
            username = username.trim();
        }

        if (password != null) {
            password = password.trim();
            if (!password.isBlank()) {
                validatePassword(password);
            }
        }
    }

    private static void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("admin.bootstrap.password must be at least 12 characters.");
        }

        String lowerCasePassword = password.toLowerCase(Locale.ROOT);
        boolean containsForbiddenPart = FORBIDDEN_PASSWORD_PARTS.stream()
                .anyMatch(lowerCasePassword::contains);
        if (containsForbiddenPart) {
            throw new IllegalArgumentException("admin.bootstrap.password contains a forbidden weak password pattern.");
        }
    }
}
