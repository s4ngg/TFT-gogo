package com.tftgogo.global.security.oauth;

import com.tftgogo.global.config.OAuth2RedirectProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
