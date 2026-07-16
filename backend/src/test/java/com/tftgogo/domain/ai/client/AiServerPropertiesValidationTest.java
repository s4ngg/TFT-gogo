package com.tftgogo.domain.ai.client;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AiServerPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void AI_서버_URL_누락_시_검증_메시지를_반환한다() {
        // given
        AiServerProperties properties = new AiServerProperties();
        properties.setInternalSecret("test-internal-secret");

        // when
        Set<String> messages = validator.validate(properties)
                .stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());

        // then
        assertThat(messages).contains("ai.server.url 설정은 필수입니다.");
    }
}
