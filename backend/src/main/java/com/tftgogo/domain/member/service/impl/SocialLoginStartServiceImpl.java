package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.service.SocialLoginStartService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SocialLoginStartServiceImpl implements SocialLoginStartService {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Override
    public SocialLoginStartResponse getStartUrl(String provider, String baseUrl) {
        SocialProvider socialProvider = SocialProvider.fromRegistrationId(provider);
        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();

        if (repository == null || repository.findByRegistrationId(socialProvider.registrationId()) == null) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED);
        }

        String authorizationUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(baseUrl))
                .path("/oauth2/authorization/{provider}")
                .buildAndExpand(socialProvider.registrationId())
                .toUriString();

        return SocialLoginStartResponse.of(authorizationUrl);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return baseUrl.trim();
    }
}
