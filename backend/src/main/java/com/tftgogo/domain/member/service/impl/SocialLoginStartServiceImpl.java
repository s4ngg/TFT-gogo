package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.service.SocialLoginStartService;
import com.tftgogo.global.config.OAuth2RedirectProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.security.oauth.OAuth2UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SocialLoginStartServiceImpl implements SocialLoginStartService {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final OAuth2RedirectProperties redirectProperties;

    @Override
    public SocialLoginStartResponse getStartUrl(String provider, String requestBaseUrl) {
        SocialProvider socialProvider = SocialProvider.fromRegistrationId(provider);
        if (socialProvider == SocialProvider.KAKAO) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();

        if (repository == null || repository.findByRegistrationId(socialProvider.registrationId()) == null) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED);
        }

        String authorizationUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(resolveAuthorizationBaseUrl(requestBaseUrl)))
                .path("/oauth2/authorization/{provider}")
                .buildAndExpand(socialProvider.registrationId())
                .toUriString();

        return SocialLoginStartResponse.of(authorizationUrl);
    }

    private String resolveAuthorizationBaseUrl(String requestBaseUrl) {
        String authorizationBaseUri = redirectProperties.getAuthorizationBaseUri();

        return authorizationBaseUri == null || authorizationBaseUri.isBlank()
                ? requestBaseUrl
                : authorizationBaseUri;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return OAuth2UrlValidator.normalizeHttpOrHttpsAbsoluteUrl(baseUrl, ErrorCode.INVALID_INPUT);
    }
}
