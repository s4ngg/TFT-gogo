package com.tftgogo.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2RedirectProductionValidatorTest {

    @Test
    void prod_profile_requires_production_authorization_base_uri() {
        OAuth2RedirectProperties properties = productionProperties();
        properties.setAuthorizationBaseUri("http://localhost:8080");

        OAuth2RedirectProductionValidator validator = new OAuth2RedirectProductionValidator(properties, prodEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth2.authorization-base-uri")
                .hasMessageContaining("https://tftgogo.com");
    }

    @Test
    void prod_profile_requires_production_authorized_redirect_uri() {
        OAuth2RedirectProperties properties = productionProperties();
        properties.setAuthorizedRedirectUri("http://localhost:5173/oauth/callback");

        OAuth2RedirectProductionValidator validator = new OAuth2RedirectProductionValidator(properties, prodEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.oauth2.authorized-redirect-uri")
                .hasMessageContaining("https://tftgogo.com/oauth/callback");
    }

    @Test
    void prod_profile_accepts_production_oauth2_redirect_contract() {
        OAuth2RedirectProductionValidator validator = new OAuth2RedirectProductionValidator(
                productionProperties(),
                prodEnvironment()
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void non_prod_profile_allows_local_oauth2_redirects() {
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizationBaseUri("http://localhost:8080");
        properties.setAuthorizedRedirectUri("http://localhost:5173/oauth/callback");
        properties.setLoginFailureRedirectUri("http://localhost:5173/login");

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        OAuth2RedirectProductionValidator validator = new OAuth2RedirectProductionValidator(properties, environment);

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
