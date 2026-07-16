package com.tftgogo.global.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String COOKIE_NAME = "OAUTH2_AUTH_REQUEST";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TEST_SECRET = "test-jwt-secret-value-must-be-longer-than-32";

    private ObjectMapper objectMapper;
    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        objectMapper = new ObjectMapper();
        repository = new CookieOAuth2AuthorizationRequestRepository(jwtProperties, objectMapper);
    }

    @Test
    void 인증요청을_쿠키에_저장하고_다시_로드한다() {
        // given
        MockHttpServletRequest saveRequest = secureRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();

        // when
        repository.saveAuthorizationRequest(authorizationRequest, saveRequest, response);
        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(new Cookie(COOKIE_NAME, extractCookieValue(response)));
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        // then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getAuthorizationUri()).isEqualTo(authorizationRequest.getAuthorizationUri());
        assertThat(loaded.getClientId()).isEqualTo(authorizationRequest.getClientId());
        assertThat(loaded.getRedirectUri()).isEqualTo(authorizationRequest.getRedirectUri());
        assertThat(loaded.getScopes()).isEqualTo(authorizationRequest.getScopes());
        assertThat(loaded.getState()).isEqualTo(authorizationRequest.getState());
    }

    @Test
    void 전달받은_프로토콜이_https이면_Secure_쿠키를_설정한다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        // then
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Secure");
    }

    @Test
    void 서명이_변조된_쿠키는_로드하지_않는다() {
        // given
        MockHttpServletRequest saveRequest = secureRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest(), saveRequest, response);

        String cookieValue = extractCookieValue(response);
        String tamperedCookieValue = cookieValue.substring(0, cookieValue.length() - 1) + "x";
        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(new Cookie(COOKIE_NAME, tamperedCookieValue));

        // when
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        // then
        assertThat(loaded).isNull();
    }

    @Test
    void 만료된_쿠키는_로드하지_않는다() throws Exception {
        // given
        String expiredCookieValue = signedCookieValue(Instant.now().minusSeconds(1));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, expiredCookieValue));

        // when
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNull();
    }

    @Test
    void 크기_제한을_초과한_쿠키는_로드하지_않는다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, "a".repeat(3501)));

        // when
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        // then
        assertThat(loaded).isNull();
    }

    @Test
    void 저장할_인증요청이_쿠키_크기_제한을_초과하면_예외를_던진다() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.example/oauth2/authorize")
                .clientId("client-id")
                .redirectUri("https://tftgogo.com/login/oauth2/code/google")
                .scopes(Set.of("profile", "email"))
                .state("state-value")
                .additionalParameters(parameters -> parameters.put("large", "x".repeat(5000)))
                .authorizationRequestUri("https://provider.example/oauth2/authorize?client_id=client-id")
                .attributes(attributes -> attributes.put("registration_id", "google"))
                .build();

        // when & then
        assertThatThrownBy(() -> repository.saveAuthorizationRequest(
                authorizationRequest,
                secureRequest(),
                new MockHttpServletResponse()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("OAuth2 authorization request cookie exceeds maximum size.");
    }

    private MockHttpServletRequest secureRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        return request;
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.example/oauth2/authorize")
                .clientId("client-id")
                .redirectUri("https://tftgogo.com/login/oauth2/code/google")
                .scopes(Set.of("profile", "email"))
                .state("state-value")
                .additionalParameters(parameters -> parameters.put("prompt", "select_account"))
                .authorizationRequestUri("https://provider.example/oauth2/authorize?client_id=client-id")
                .attributes(attributes -> attributes.put("registration_id", "google"))
                .build();
    }

    private String extractCookieValue(MockHttpServletResponse response) {
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).startsWith(COOKIE_NAME + "=");
        return setCookie.substring((COOKIE_NAME + "=").length(), setCookie.indexOf(";"));
    }

    private String signedCookieValue(Instant expiresAt) throws Exception {
        Map<String, Object> cookieValue = Map.of(
                "authorizationUri", "https://provider.example/oauth2/authorize",
                "clientId", "client-id",
                "redirectUri", "https://tftgogo.com/login/oauth2/code/google",
                "scopes", Set.of("profile", "email"),
                "state", "state-value",
                "additionalParameters", Map.of("prompt", "select_account"),
                "authorizationRequestUri", "https://provider.example/oauth2/authorize?client_id=client-id",
                "attributes", Map.of("registration_id", "google"),
                "expiresAtEpochSecond", expiresAt.getEpochSecond()
        );

        String payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(cookieValue));
        return payload + "." + sign(payload);
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return Base64.getUrlEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
