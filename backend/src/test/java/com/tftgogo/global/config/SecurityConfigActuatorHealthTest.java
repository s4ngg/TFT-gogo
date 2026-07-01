package com.tftgogo.global.config;

import com.tftgogo.domain.admin.service.AdminAuditService;
import com.tftgogo.global.filter.AdminJwtFilter;
import com.tftgogo.global.filter.JwtAuthenticationFilter;
import com.tftgogo.global.security.oauth.CookieOAuth2AuthorizationRequestRepository;
import com.tftgogo.global.security.oauth.SocialOAuth2FailureHandler;
import com.tftgogo.global.security.oauth.SocialOAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigActuatorHealthTest.HealthController.class)
@Import({SecurityConfig.class, SecurityConfigActuatorHealthTest.TestConfig.class})
class SecurityConfigActuatorHealthTest {

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

    @MockBean
    private CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Test
    void actuator_health_path_is_public_for_alb_health_check() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @RestController
    static class HealthController {

        @GetMapping("/actuator/health")
        String health() {
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
