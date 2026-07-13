package com.tftgogo.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2RedirectPropertiesProductionValidatorTest {

    @Test
    void prod_profile_accepts_tftgogo_production_oauth_urls() {
        OAuth2RedirectProperties properties = productionProperties();
        MockEnvironment environment = prodEnvironment();

        OAuth2RedirectPropertiesProductionValidator validator =
                new OAuth2RedirectPropertiesProductionValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void prod_profile_rejects_localhost_authorization_base_uri() {
        OAuth2RedirectProperties properties = productionProperties();
        properties.setAuthorizationBaseUri("http://localhost:8080");
        MockEnvironment environment = prodEnvironment();

        OAuth2RedirectPropertiesProductionValidator validator =
                new OAuth2RedirectPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authorization-base-uri");
    }

    @Test
    void prod_profile_rejects_http_authorized_redirect_uri() {
        OAuth2RedirectProperties properties = productionProperties();
        properties.setAuthorizedRedirectUri("http://tftgogo.com/oauth/callback");
        MockEnvironment environment = prodEnvironment();

        OAuth2RedirectPropertiesProductionValidator validator =
                new OAuth2RedirectPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authorized-redirect-uri");
    }

    @Test
    void prod_profile_rejects_localhost_login_failure_redirect_uri() {
        OAuth2RedirectProperties properties = productionProperties();
        properties.setLoginFailureRedirectUri("http://localhost:5173/login");
        MockEnvironment environment = prodEnvironment();

        OAuth2RedirectPropertiesProductionValidator validator =
                new OAuth2RedirectPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("login-failure-redirect-uri");
    }

    @Test
    void non_prod_profile_allows_local_oauth_urls() {
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizationBaseUri("http://localhost:8080");
        properties.setAuthorizedRedirectUri("http://localhost:5173/oauth/callback");
        properties.setLoginFailureRedirectUri("http://localhost:5173/login");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        OAuth2RedirectPropertiesProductionValidator validator =
                new OAuth2RedirectPropertiesProductionValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private static OAuth2RedirectProperties productionProperties() {
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizationBaseUri("https://tftgogo.com");
        properties.setAuthorizedRedirectUri("https://tftgogo.com/oauth/callback");
        properties.setLoginFailureRedirectUri("https://tftgogo.com/login");
        return properties;
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
