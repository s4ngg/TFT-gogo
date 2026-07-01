package com.tftgogo.global.security.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final Duration AUTHORIZATION_REQUEST_COOKIE_MAX_AGE = Duration.ofMinutes(3);
    private static final int AUTHORIZATION_REQUEST_COOKIE_VALUE_MAX_BYTES = 3500;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String COOKIE_VALUE_SEPARATOR = ".";

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }

        return deserialize(cookie.getValue());
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }

        String serializedAuthorizationRequest = serialize(authorizationRequest);
        validateCookieSize(serializedAuthorizationRequest);

        ResponseCookie cookie = ResponseCookie.from(
                        AUTHORIZATION_REQUEST_COOKIE_NAME,
                        serializedAuthorizationRequest
                )
                .path("/")
                .maxAge(AUTHORIZATION_REQUEST_COOKIE_MAX_AGE)
                .httpOnly(true)
                .secure(isSecureCookie(request))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response);
        return authorizationRequest;
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_REQUEST_COOKIE_NAME, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .secure(isSecureCookie(request))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            AuthorizationRequestCookieValue cookieValue =
                    AuthorizationRequestCookieValue.from(
                            authorizationRequest,
                            Instant.now().plus(AUTHORIZATION_REQUEST_COOKIE_MAX_AGE)
                    );
            String payload = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cookieValue));
            return payload + COOKIE_VALUE_SEPARATOR + sign(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OAuth2 authorization request cookie serialization failed.", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > AUTHORIZATION_REQUEST_COOKIE_VALUE_MAX_BYTES) {
            return null;
        }

        String[] parts = value.split("\\.", 2);
        if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            AuthorizationRequestCookieValue cookieValue =
                    objectMapper.readValue(decoded, AuthorizationRequestCookieValue.class);
            if (cookieValue.isExpired()) {
                return null;
            }
            return cookieValue.toAuthorizationRequest();
        } catch (IllegalArgumentException | IOException e) {
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return Base64.getUrlEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("OAuth2 authorization request cookie signing failed.", e);
        }
    }

    private boolean isValidSignature(String payload, String signature) {
        return MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isSecureCookie(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private void validateCookieSize(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > AUTHORIZATION_REQUEST_COOKIE_VALUE_MAX_BYTES) {
            throw new IllegalStateException("OAuth2 authorization request cookie exceeds maximum size.");
        }
    }

    private record AuthorizationRequestCookieValue(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            String authorizationRequestUri,
            Map<String, Object> attributes,
            long expiresAtEpochSecond
    ) {

        private static AuthorizationRequestCookieValue from(
                OAuth2AuthorizationRequest authorizationRequest,
                Instant expiresAt
        ) {
            return new AuthorizationRequestCookieValue(
                    authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(),
                    authorizationRequest.getRedirectUri(),
                    authorizationRequest.getScopes(),
                    authorizationRequest.getState(),
                    authorizationRequest.getAdditionalParameters(),
                    authorizationRequest.getAuthorizationRequestUri(),
                    authorizationRequest.getAttributes(),
                    expiresAt.getEpochSecond()
            );
        }

        private OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes == null ? Set.of() : scopes)
                    .state(state)
                    .additionalParameters(parameters -> parameters.putAll(
                            additionalParameters == null ? Map.of() : additionalParameters
                    ))
                    .authorizationRequestUri(authorizationRequestUri)
                    .attributes(requestAttributes -> requestAttributes.putAll(
                            attributes == null ? Map.of() : attributes
                    ))
                    .build();
        }

        private boolean isExpired() {
            return !Instant.now().isBefore(Instant.ofEpochSecond(expiresAtEpochSecond));
        }
    }
}
