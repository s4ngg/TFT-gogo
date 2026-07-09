package com.tftgogo.domain.admin.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBootstrapPropertiesTest {

    @Test
    void 비밀번호가_없으면_부트스트랩_스킵을_위해_허용한다() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(null, " ");

        assertThat(properties.username()).isEqualTo("admin");
        assertThat(properties.password()).isBlank();
    }

    @Test
    void 안전한_관리자_bootstrap_비밀번호는_허용한다() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(
                " master ",
                "Strong-Master-Key-2026!"
        );

        assertThat(properties.username()).isEqualTo("master");
        assertThat(properties.password()).isEqualTo("Strong-Master-Key-2026!");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short",
            "changeme-2026!",
            "admin-master-key",
            "password-2026!",
            "tftgogo-master-key"
    })
    void 약한_관리자_bootstrap_비밀번호는_거부한다(String weakPassword) {
        assertThatThrownBy(() -> new AdminBootstrapProperties("admin", weakPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin.bootstrap.password");
    }
}
