package com.tftgogo.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesProductionValidatorTest {

    @Test
    void prod_profile_requires_production_origins() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://example.com"));
        MockEnvironment environment = prodEnvironment();

        CorsPropertiesProductionValidator validator = new CorsPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https://tftgogo.com");
    }

    @Test
    void prod_profile_accepts_required_production_origins() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://tftgogo.com", "https://www.tftgogo.com"));
        MockEnvironment environment = prodEnvironment();

        CorsPropertiesProductionValidator validator = new CorsPropertiesProductionValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void prod_profile_rejects_localhost_origin() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(
                "https://tftgogo.com",
                "https://www.tftgogo.com",
                "http://localhost:5173"
        ));
        MockEnvironment environment = prodEnvironment();

        CorsPropertiesProductionValidator validator = new CorsPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void prod_profile_rejects_wildcard_origin() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://tftgogo.com", "https://www.tftgogo.com", "*"));
        MockEnvironment environment = prodEnvironment();

        CorsPropertiesProductionValidator validator = new CorsPropertiesProductionValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    void non_prod_profile_allows_local_defaults() {
        CorsProperties properties = new CorsProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        CorsPropertiesProductionValidator validator = new CorsPropertiesProductionValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
