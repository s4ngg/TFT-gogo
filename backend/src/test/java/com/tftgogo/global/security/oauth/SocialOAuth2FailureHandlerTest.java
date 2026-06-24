package com.tftgogo.global.security.oauth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialOAuth2FailureHandlerTest {

    @Mock
    private OAuth2RedirectService redirectService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void OAuth2_실패는_provider_error로_리다이렉트하고_세션을_정리한다() throws Exception {
        // given
        SocialOAuth2FailureHandler handler = new SocialOAuth2FailureHandler(redirectService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("oauth-user", "credentials"));
        when(redirectService.buildFailureRedirectUri(SocialOAuth2ErrorCode.PROVIDER_ERROR))
                .thenReturn("http://localhost:5173/login?oauthError=provider_error");

        // when
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("oauth failed"));

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?oauthError=provider_error");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(session.isInvalid()).isTrue();
    }
}
