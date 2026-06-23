package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialLoginStartServiceImplTest {

    @Mock
    private ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @InjectMocks
    private SocialLoginStartServiceImpl service;

    @Test
    void 설정된_provider는_절대_인증시작_URL을_반환한다() {
        // given
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(clientRegistrationRepository.findByRegistrationId("google"))
                .thenReturn(mock(ClientRegistration.class));

        // when
        SocialLoginStartResponse response = service.getStartUrl("google", "http://localhost:8080");

        // then
        assertThat(response.getAuthorizationUrl())
                .isEqualTo("http://localhost:8080/oauth2/authorization/google");
    }

    @Test
    void base_URL에_context_path가_있으면_보존한다() {
        // given
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(clientRegistrationRepository.findByRegistrationId("google"))
                .thenReturn(mock(ClientRegistration.class));

        // when
        SocialLoginStartResponse response = service.getStartUrl("google", "https://api.example.com/app");

        // then
        assertThat(response.getAuthorizationUrl())
                .isEqualTo("https://api.example.com/app/oauth2/authorization/google");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://api.example.com",
            "//api.example.com",
            "https://user:pass@api.example.com",
            "https://api.example.com/oauth path"
    })
    void base_URL이_http_https_절대_URL이_아니면_INVALID_INPUT을_던진다(String invalidBaseUrl) {
        // given
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(clientRegistrationRepository.findByRegistrationId("google"))
                .thenReturn(mock(ClientRegistration.class));

        // when, then
        assertThatThrownBy(() -> service.getStartUrl("google", invalidBaseUrl))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void OAuth2_client_repository가_없으면_503_예외를_던진다() {
        // given
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(null);

        // when, then
        assertThatThrownBy(() -> service.getStartUrl("google", "http://localhost:8080"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED));
    }

    @Test
    void provider는_지원하지만_client_registration이_없으면_503_예외를_던진다() {
        // given
        when(clientRegistrationRepositoryProvider.getIfAvailable()).thenReturn(clientRegistrationRepository);
        when(clientRegistrationRepository.findByRegistrationId("naver")).thenReturn(null);

        // when, then
        assertThatThrownBy(() -> service.getStartUrl("naver", "http://localhost:8080"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED));
    }

    @Test
    void 지원하지_않는_provider는_INVALID_INPUT을_던지고_registration을_조회하지_않는다() {
        // when, then
        assertThatThrownBy(() -> service.getStartUrl("github", "http://localhost:8080"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(clientRegistrationRepositoryProvider, never()).getIfAvailable();
    }

    @Test
    void 카카오는_QA_지원_provider에서_제외되어_INVALID_INPUT을_던진다() {
        // when, then
        assertThatThrownBy(() -> service.getStartUrl("kakao", "http://localhost:8080"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(clientRegistrationRepositoryProvider, never()).getIfAvailable();
    }
}
