package com.tftgogo.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.admin.security.AdminJwtTokenProvider;
import com.tftgogo.domain.admin.entity.AdminRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminJwtFilterTest {

    private MockMvc mockMvc;
    private AdminJwtTokenProvider tokenProvider;

    @RestController
    static class DummyController {
        @GetMapping("/api/admin/decks")
        public String dummy() { return "ok"; }
    }

    @BeforeEach
    void setUp() {
        tokenProvider = Mockito.mock(AdminJwtTokenProvider.class);
        AdminJwtFilter filter = new AdminJwtFilter(tokenProvider, new ObjectMapper());

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .addFilter(filter, "/api/admin/*")
                .build();
    }

    @Test
    void Authorization_헤더가_없으면_401과_ApiResponse_형식을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/decks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("관리자 인증이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 유효하지_않은_토큰이면_401을_반환한다() throws Exception {
        when(tokenProvider.validateToken(any())).thenReturn(false);

        mockMvc.perform(get("/api/admin/decks")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 유효한_토큰이면_컨트롤러_응답을_반환한다() throws Exception {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getAdminId("valid-token")).thenReturn(1L);
        when(tokenProvider.getUsername("valid-token")).thenReturn("admin");
        when(tokenProvider.getRole("valid-token")).thenReturn(AdminRole.MASTER);

        mockMvc.perform(get("/api/admin/decks")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }
}
