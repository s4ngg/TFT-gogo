package com.tftgogo.global.config;

import com.tftgogo.domain.admin.service.AdminAuditService;
import com.tftgogo.global.filter.AdminJwtFilter;
import com.tftgogo.global.filter.JwtAuthenticationFilter;
import com.tftgogo.global.security.oauth.SocialOAuth2FailureHandler;
import com.tftgogo.global.security.oauth.SocialOAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SecurityConfigOAuth2WithoutRepositoryTest.DummyController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import({SecurityConfig.class, SecurityConfigOAuth2WithoutRepositoryTest.TestConfig.class})
class SecurityConfigOAuth2WithoutRepositoryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminJwtFilter adminJwtFilter;

    @MockBean
    private AdminAuditService adminAuditService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SocialOAuth2SuccessHandler socialOAuth2SuccessHandler;

    @MockBean
    private SocialOAuth2FailureHandler socialOAuth2FailureHandler;

    @Test
    void ClientRegistrationRepository가_없으면_OAuth2_시작경로는_redirect하지_않는다() throws Exception {
        // given
        String authorizationPath = "/oauth2/authorization/google";

        // when & then
        mockMvc.perform(get(authorizationPath))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));

        verifyNoInteractions(socialOAuth2SuccessHandler, socialOAuth2FailureHandler);
    }

    @RestController
    static class DummyController {

        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOrigins(List.of("http://localhost:5173"));
            return properties;
        }
    }
}
