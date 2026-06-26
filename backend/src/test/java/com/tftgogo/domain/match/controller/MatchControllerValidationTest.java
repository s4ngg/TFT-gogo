package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.service.MatchService;
import com.tftgogo.global.cdragon.service.TftAssetCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MatchController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.tftgogo\\.global\\.(filter|security|config\\.Security).*"
        )
)
class MatchControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private TftAssetCacheService tftAssetCacheService;

    @Test
    void start가_음수이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", "-1")
                        .param("count", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(matchService);
    }

    @Test
    void start가_200초과이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", "261")
                        .param("count", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(matchService);
    }

    @Test
    void count가_0이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", "0")
                        .param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(matchService);
    }

    @Test
    void count가_21이면_400을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", "0")
                        .param("count", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(matchService);
    }
}
