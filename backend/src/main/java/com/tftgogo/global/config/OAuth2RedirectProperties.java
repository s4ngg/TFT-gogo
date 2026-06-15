package com.tftgogo.global.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2RedirectProperties {

    @NotBlank(message = "소셜 로그인 성공 리다이렉트 URI는 필수입니다.")
    private String authorizedRedirectUri;

    @NotBlank(message = "소셜 로그인 실패 리다이렉트 URI는 필수입니다.")
    private String loginFailureRedirectUri;
}
