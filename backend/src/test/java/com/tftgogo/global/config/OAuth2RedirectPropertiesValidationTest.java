package com.tftgogo.global.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RedirectPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void OAuth2_리다이렉트_URI_누락_검증_메시지는_한글로_유지된다() {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();

        // when
        Set<String> messages = validator.validate(properties)
                .stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());

        // then
        assertThat(messages).contains(
                "소셜 로그인 성공 리다이렉트 URI는 필수입니다.",
                "소셜 로그인 실패 리다이렉트 URI는 필수입니다."
        );
    }
}
