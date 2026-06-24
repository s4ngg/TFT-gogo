package com.tftgogo.global.security.oauth;

import com.tftgogo.global.config.OAuth2RedirectProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2RedirectServiceTest {

    @Test
    void 성공_리다이렉트는_fragment에_accessToken만_담는다() {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizedRedirectUri("http://localhost:5173/oauth/callback");
        OAuth2RedirectService service = new OAuth2RedirectService(properties);

        // when
        String redirectUri = service.buildSuccessRedirectUri("access.token.value");

        // then
        assertThat(redirectUri).isEqualTo("http://localhost:5173/oauth/callback#accessToken=access.token.value");
    }

    @Test
    void 성공_리다이렉트는_fragment의_accessToken을_인코딩한다() {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizedRedirectUri("http://localhost:5173/oauth/callback");
        OAuth2RedirectService service = new OAuth2RedirectService(properties);

        // when
        String redirectUri = service.buildSuccessRedirectUri("access+/=token");

        // then
        assertThat(redirectUri)
                .isEqualTo("http://localhost:5173/oauth/callback#accessToken=access%2B%2F%3Dtoken");
    }

    @Test
    void 실패_리다이렉트는_허용된_oauthError_code만_담는다() {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setLoginFailureRedirectUri("http://localhost:5173/login");
        OAuth2RedirectService service = new OAuth2RedirectService(properties);

        // when
        String redirectUri = service.buildFailureRedirectUri(SocialOAuth2ErrorCode.EMAIL_EXISTS);

        // then
        assertThat(redirectUri).isEqualTo("http://localhost:5173/login?oauthError=email_exists");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/oauth/callback",
            "//frontend.example.com/oauth/callback",
            "javascript:alert(1)",
            "https://user:pass@frontend.example.com/oauth/callback",
            "https://frontend.example.com/oauth callback"
    })
    void 성공_리다이렉트_URI가_http_https_절대_URI가_아니면_INVALID_INPUT을_던진다(String invalidRedirectUri) {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setAuthorizedRedirectUri(invalidRedirectUri);
        OAuth2RedirectService service = new OAuth2RedirectService(properties);

        // when, then
        assertThatThrownBy(() -> service.buildSuccessRedirectUri("access.token.value"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/login",
            "//frontend.example.com/login",
            "data:text/plain,error",
            "https://user:pass@frontend.example.com/login",
            "https://frontend.example.com/login path"
    })
    void 실패_리다이렉트_URI가_http_https_절대_URI가_아니면_INVALID_INPUT을_던진다(String invalidRedirectUri) {
        // given
        OAuth2RedirectProperties properties = new OAuth2RedirectProperties();
        properties.setLoginFailureRedirectUri(invalidRedirectUri);
        OAuth2RedirectService service = new OAuth2RedirectService(properties);

        // when, then
        assertThatThrownBy(() -> service.buildFailureRedirectUri(SocialOAuth2ErrorCode.PROVIDER_ERROR))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
