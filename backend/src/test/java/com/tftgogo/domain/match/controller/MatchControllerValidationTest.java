package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.service.MatchService;
import com.tftgogo.global.cdragon.service.TftAssetCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MatchController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        },
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
                        .param("start", "201")
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

    @ParameterizedTest
    @ValueSource(ints = {0, 100, 200})
    void start_경계값이_유효하면_정상_통과한다(int start) throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", String.valueOf(start))
                        .param("count", "20"))
                .andExpect(status().isOk());

        verify(matchService).getMatches(eq("test-puuid"), eq(start), eq(20), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20})
    void count_경계값이_유효하면_정상_통과한다(int count) throws Exception {
        mockMvc.perform(get("/api/match/{puuid}/matches", "test-puuid")
                        .param("start", "0")
                        .param("count", String.valueOf(count)))
                .andExpect(status().isOk());

        verify(matchService).getMatches(eq("test-puuid"), eq(0), eq(count), any(), any(), any());
    }
}
