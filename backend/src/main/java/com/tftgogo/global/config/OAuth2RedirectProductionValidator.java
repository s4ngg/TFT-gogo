package com.tftgogo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2RedirectProductionValidator implements ApplicationRunner {

    private static final String PRODUCTION_AUTHORIZATION_BASE_URI = "https://tftgogo.com";
    private static final String PRODUCTION_AUTHORIZED_REDIRECT_URI = "https://tftgogo.com/oauth/callback";
    private static final String PRODUCTION_LOGIN_FAILURE_REDIRECT_URI = "https://tftgogo.com/login";

    private final OAuth2RedirectProperties properties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        validateExactUri(
                "app.oauth2.authorization-base-uri",
                properties.getAuthorizationBaseUri(),
                PRODUCTION_AUTHORIZATION_BASE_URI
        );
        validateExactUri(
                "app.oauth2.authorized-redirect-uri",
                properties.getAuthorizedRedirectUri(),
                PRODUCTION_AUTHORIZED_REDIRECT_URI
        );
        validateExactUri(
                "app.oauth2.login-failure-redirect-uri",
                properties.getLoginFailureRedirectUri(),
                PRODUCTION_LOGIN_FAILURE_REDIRECT_URI
        );
    }

    private static void validateExactUri(String propertyName, String actualValue, String expectedValue) {
        String normalizedActualValue = normalize(actualValue);
        if (!expectedValue.equals(normalizedActualValue)) {
            throw new IllegalStateException(propertyName + " must be " + expectedValue + " when prod profile is active.");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.endsWith("/") && normalized.length() > 1) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
