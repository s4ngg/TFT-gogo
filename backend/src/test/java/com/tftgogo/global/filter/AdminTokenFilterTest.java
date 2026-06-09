package com.tftgogo.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminTokenFilterTest {

    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @GetMapping("/api/admin/decks")
        public String dummy() { return "ok"; }
    }

    @BeforeEach
    void setUp() {
        AdminTokenFilter filter = new AdminTokenFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "adminSecretToken", "test-secret");

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .addFilter(filter, "/api/admin/*")
                .build();
    }

    @Test
    void 토큰이_없으면_401과_ApiResponse_형식을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/decks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("관리자 인증이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 토큰이_틀리면_401과_ApiResponse_형식을_반환한다() throws Exception {
        mockMvc.perform(get("/api/admin/decks")
                        .header("X-Admin-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("관리자 인증이 필요합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
