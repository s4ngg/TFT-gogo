package com.tftgogo.domain.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.bootstrap")
public record AdminBootstrapProperties(
        String username,
        String password
) {
    public AdminBootstrapProperties {
        if (username == null || username.isBlank()) username = "admin";
    }
}
