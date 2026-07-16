package com.tftgogo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class CorsPropertiesProductionValidator implements ApplicationRunner {

    private static final List<String> REQUIRED_PRODUCTION_ORIGINS = List.of(
            "https://tftgogo.com",
            "https://www.tftgogo.com"
    );

    private final CorsProperties corsProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        List<String> allowedOrigins = normalizedAllowedOrigins();
        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins must be set when prod profile is active.");
        }

        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException("app.cors.allowed-origins must not contain wildcard in prod profile.");
        }

        boolean hasLocalOrigin = allowedOrigins.stream().anyMatch(CorsPropertiesProductionValidator::isLocalOrigin);
        if (hasLocalOrigin) {
            throw new IllegalStateException("app.cors.allowed-origins must not contain localhost in prod profile.");
        }

        if (!allowedOrigins.containsAll(REQUIRED_PRODUCTION_ORIGINS)) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must contain https://tftgogo.com and https://www.tftgogo.com in prod profile."
            );
        }
    }

    private List<String> normalizedAllowedOrigins() {
        if (corsProperties.getAllowedOrigins() == null) {
            return List.of();
        }

        return corsProperties.getAllowedOrigins().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private static boolean isLocalOrigin(String origin) {
        return origin.startsWith("http://localhost")
                || origin.startsWith("http://127.0.0.1");
    }
}
