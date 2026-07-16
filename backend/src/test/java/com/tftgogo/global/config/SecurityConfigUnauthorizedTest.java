package com.tftgogo.global.config;

import com.tftgogo.domain.admin.security.AdminJwtTokenProvider;
import com.tftgogo.domain.admin.service.AdminAuditService;
import com.tftgogo.domain.member.service.impl.AuthTokenService;
import com.tftgogo.global.filter.AdminJwtFilter;
import com.tftgogo.global.filter.JwtAuthenticationFilter;
import com.tftgogo.global.security.ApiAuthenticationEntryPoint;
import com.tftgogo.global.security.JwtTokenProvider;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigUnauthorizedTest.ProtectedController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        AdminJwtFilter.class,
        JwtAuthenticationFilter.class,
        SecurityConfigUnauthorizedTest.TestConfig.class
})
class SecurityConfigUnauthorizedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminJwtTokenProvider adminJwtTokenProvider;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthTokenService authTokenService;

    @MockBean
    private AdminAuditService adminAuditService;

    @MockBean
    private SocialOAuth2SuccessHandler socialOAuth2SuccessHandler;

    @MockBean
    private SocialOAuth2FailureHandler socialOAuth2FailureHandler;

    @MockBean
    private CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Test
    void 보호된_API에_토큰이_없으면_ApiResponse_형식의_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 보호된_API에_무효한_사용자_JWT를_보내면_403이_아닌_401을_반환한다() throws Exception {
        when(adminJwtTokenProvider.isAdminToken("invalid-token")).thenReturn(false);
        when(authTokenService.isAccessTokenUsable("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

        verify(authTokenService).isAccessTokenUsable("invalid-token");
    }

    @RestController
    static class ProtectedController {

        @GetMapping("/api/test/protected")
        String protectedApi() {
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
