package com.tftgogo.global.config;

import com.tftgogo.global.filter.AdminTokenFilter;
import com.tftgogo.global.filter.JwtAuthenticationFilter;
import com.tftgogo.global.security.oauth.SocialOAuth2FailureHandler;
import com.tftgogo.global.security.oauth.SocialOAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigOAuth2Test.DummyController.class)
@Import({SecurityConfig.class, SecurityConfigOAuth2Test.OAuth2TestConfig.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class SecurityConfigOAuth2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTokenFilter adminTokenFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SocialOAuth2SuccessHandler socialOAuth2SuccessHandler;

    @MockBean
    private SocialOAuth2FailureHandler socialOAuth2FailureHandler;

    @Test
    void OAuth2_인증시작_경로는_fallback_denyAll이_아니라_provider로_리다이렉트한다() throws Exception {
        // given
        String authorizationPath = "/oauth2/authorization/google";
        String expectedGoogleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        String expectedClientId = "client_id=test-client-id";

        // when & then
        mockMvc.perform(get(authorizationPath))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString(expectedGoogleAuthUrl)))
                .andExpect(header().string("Location", containsString(expectedClientId)));
    }

    @RestController
    static class DummyController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }

    @TestConfiguration
    static class OAuth2TestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(googleRegistration());
        }

        @Bean
        CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOrigins(List.of("http://localhost:5173"));
            return properties;
        }

        private static ClientRegistration googleRegistration() {
            return ClientRegistration.withRegistrationId("google")
                    .clientId("test-client-id")
                    .clientSecret("test-client-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .userNameAttributeName("sub")
                    .clientName("Google")
                    .build();
        }
    }
}
