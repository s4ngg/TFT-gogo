package com.tftgogo.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OAuth2RedirectPropertiesProductionValidator implements ApplicationRunner {

    private static final String REQUIRED_AUTHORIZATION_BASE_URI = "https://tftgogo.com";
    private static final String REQUIRED_AUTHORIZED_REDIRECT_URI = "https://tftgogo.com/oauth/callback";
    private static final String REQUIRED_LOGIN_FAILURE_REDIRECT_URI = "https://tftgogo.com/login";

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

        requireExact(
                normalize(properties.getAuthorizationBaseUri()),
                REQUIRED_AUTHORIZATION_BASE_URI,
                "app.oauth2.authorization-base-uri must be https://tftgogo.com when prod profile is active."
        );
        requireExact(
                normalize(properties.getAuthorizedRedirectUri()),
                REQUIRED_AUTHORIZED_REDIRECT_URI,
                "app.oauth2.authorized-redirect-uri must be https://tftgogo.com/oauth/callback when prod profile is active."
        );
        requireExact(
                normalize(properties.getLoginFailureRedirectUri()),
                REQUIRED_LOGIN_FAILURE_REDIRECT_URI,
                "app.oauth2.login-failure-redirect-uri must be https://tftgogo.com/login when prod profile is active."
        );
    }

    private static void requireExact(String actual, String expected, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > "https://".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
