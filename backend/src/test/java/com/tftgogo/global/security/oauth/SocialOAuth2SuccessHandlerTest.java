package com.tftgogo.global.security.oauth;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialOAuth2SuccessHandlerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private OAuth2RedirectService redirectService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 성공하면_소셜명령으로_로그인하고_accessToken_fragment로_리다이렉트한다() throws Exception {
        // given
        SocialOAuth2SuccessHandler handler = new SocialOAuth2SuccessHandler(memberService, redirectService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("oauth-user", "credentials"));
        AuthResponse authResponse = AuthResponse.of(
                "access.token.value",
                MemberResponse.builder()
                        .id(1L)
                        .email("google@example.com")
                        .nickname("Google User")
                        .notificationEnabled(true)
                        .build()
        );
        when(memberService.socialLogin(any(SocialLoginCommand.class))).thenReturn(authResponse);
        when(redirectService.buildSuccessRedirectUri("access.token.value"))
                .thenReturn("http://localhost:5173/oauth/callback#accessToken=access.token.value");

        // when
        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        // then
        ArgumentCaptor<SocialLoginCommand> commandCaptor = ArgumentCaptor.forClass(SocialLoginCommand.class);
        verify(memberService).socialLogin(commandCaptor.capture());
        SocialLoginCommand command = commandCaptor.getValue();
        assertThat(command.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(command.getSocialId()).isEqualTo("google-sub");
        assertThat(command.getEmail()).isEqualTo("google@example.com");
        assertThat(command.getNickname()).isEqualTo("Google User");
        assertThat(command.getProfileImage()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth/callback#accessToken=access.token.value");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 같은_이메일_일반회원이_있으면_email_exists로_리다이렉트한다() throws Exception {
        // given
        SocialOAuth2SuccessHandler handler = new SocialOAuth2SuccessHandler(memberService, redirectService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("oauth-user", "credentials"));
        when(memberService.socialLogin(any(SocialLoginCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS));
        when(redirectService.buildFailureRedirectUri(SocialOAuth2ErrorCode.EMAIL_EXISTS))
                .thenReturn("http://localhost:5173/login?oauthError=email_exists");

        // when
        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?oauthError=email_exists");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void provider_email이_없으면_email_required로_리다이렉트한다() throws Exception {
        // given
        SocialOAuth2SuccessHandler handler = new SocialOAuth2SuccessHandler(memberService, redirectService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("oauth-user", "credentials"));
        when(memberService.socialLogin(any(SocialLoginCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED));
        when(redirectService.buildFailureRedirectUri(SocialOAuth2ErrorCode.EMAIL_REQUIRED))
                .thenReturn("http://localhost:5173/login?oauthError=email_required");

        // when
        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?oauthError=email_required");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 예상하지_못한_예외는_provider_error로_리다이렉트한다() throws Exception {
        // given
        SocialOAuth2SuccessHandler handler = new SocialOAuth2SuccessHandler(memberService, redirectService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("oauth-user", "credentials"));
        when(memberService.socialLogin(any(SocialLoginCommand.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));
        when(redirectService.buildFailureRedirectUri(SocialOAuth2ErrorCode.PROVIDER_ERROR))
                .thenReturn("http://localhost:5173/login?oauthError=provider_error");

        // when
        handler.onAuthenticationSuccess(request, response, googleAuthentication());

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?oauthError=provider_error");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(session.isInvalid()).isTrue();
    }

    private OAuth2AuthenticationToken googleAuthentication() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", "google-sub",
                        "email", "google@example.com",
                        "email_verified", true,
                        "name", "Google User",
                        "picture", "https://cdn.example.com/profile.png"
                ),
                "sub"
        );

        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }
}
