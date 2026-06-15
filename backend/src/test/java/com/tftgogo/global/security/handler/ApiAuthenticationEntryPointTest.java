package com.tftgogo.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationEntryPointTest {

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(new ObjectMapper());

    @Test
    void 인증정보가_없으면_401과_ApiResponse_형식을_반환한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Full authentication is required")
        );

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentAsString())
                .contains("\"success\":false")
                .contains("\"message\":\"인증이 필요합니다.\"")
                .doesNotContain("Full authentication is required");
    }
}
