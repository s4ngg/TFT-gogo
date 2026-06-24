package com.tftgogo.global.config;

import com.tftgogo.global.security.oauth.OAuth2UrlValidator;
import jakarta.validation.constraints.AssertTrue;
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

    private String authorizationBaseUri;

    @AssertTrue(message = "소셜 로그인 성공 리다이렉트 URI는 http/https 절대 URI여야 합니다.")
    public boolean isAuthorizedRedirectUriValid() {
        return OAuth2UrlValidator.isHttpOrHttpsAbsoluteUrl(authorizedRedirectUri);
    }

    @AssertTrue(message = "소셜 로그인 실패 리다이렉트 URI는 http/https 절대 URI여야 합니다.")
    public boolean isLoginFailureRedirectUriValid() {
        return OAuth2UrlValidator.isHttpOrHttpsAbsoluteUrl(loginFailureRedirectUri);
    }

    @AssertTrue(message = "소셜 로그인 시작 기준 URI는 비어 있거나 http/https 절대 URI여야 합니다.")
    public boolean isAuthorizationBaseUriValid() {
        return authorizationBaseUri == null
                || authorizationBaseUri.isBlank()
                || OAuth2UrlValidator.isHttpOrHttpsAbsoluteUrl(authorizationBaseUri);
    }
}
