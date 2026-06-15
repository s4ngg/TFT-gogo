package com.tftgogo.global.security.oauth;

import com.tftgogo.global.config.OAuth2RedirectProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2RedirectService {

    private static final String ACCESS_TOKEN_PARAM = "accessToken";
    private static final String OAUTH_ERROR_PARAM = "oauthError";

    private final OAuth2RedirectProperties properties;

    public String buildSuccessRedirectUri(String accessToken) {
        String fragment = ACCESS_TOKEN_PARAM + "=" + UriUtils.encode(accessToken, StandardCharsets.UTF_8);

        return UriComponentsBuilder
                .fromUriString(properties.getAuthorizedRedirectUri())
                .fragment(fragment)
                .build(true)
                .toUriString();
    }

    public String buildFailureRedirectUri(SocialOAuth2ErrorCode errorCode) {
        return UriComponentsBuilder
                .fromUriString(properties.getLoginFailureRedirectUri())
                .queryParam(OAUTH_ERROR_PARAM, errorCode.value())
                .build()
                .toUriString();
    }
}
