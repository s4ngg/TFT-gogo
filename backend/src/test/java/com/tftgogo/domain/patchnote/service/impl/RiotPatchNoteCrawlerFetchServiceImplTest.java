package com.tftgogo.domain.patchnote.service.impl;

import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiotPatchNoteCrawlerFetchServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private PatchNoteCrawlerProperties properties = new PatchNoteCrawlerProperties();

    @InjectMocks
    private RiotPatchNoteCrawlerFetchServiceImpl fetchService;

    @BeforeEach
    void setUp() {
        properties.setUserAgent("TFT-gogo-test/1.0");
    }

    @Test
    void fetch_whenOfficialUrl_sendsUserAgentAndReturnsHtml() {
        // given
        String sourceUrl = "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/";
        when(restTemplate.exchange(eq(sourceUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("<html>ok</html>"));

        // when
        PatchNoteCrawlFetchedPage response = fetchService.fetch(sourceUrl);

        // then
        assertThat(response.sourceUrl()).isEqualTo(sourceUrl);
        assertThat(response.rawHtml()).isEqualTo("<html>ok</html>");
        assertThat(response.httpStatus()).isEqualTo(200);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(sourceUrl), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));
        assertThat(entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.USER_AGENT))
                .isEqualTo("TFT-gogo-test/1.0");
    }

    @Test
    void fetch_whenHostIsNotOfficial_throwsInvalidInput() {
        // given
        String sourceUrl = "https://teamfighttactics.leagueoflegends.com.evil.example/ko-kr/news/game-updates/a";

        // when, then
        assertThatThrownBy(() -> fetchService.fetch(sourceUrl))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchTagPage_usesConfiguredLocale() {
        // given
        String expectedUrl = "https://www.leagueoflegends.com/en-us/news/tags/teamfight-tactics-patch-notes/";
        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("<html>list</html>"));

        // when
        PatchNoteCrawlFetchedPage response = fetchService.fetchTagPage("EN-US");

        // then
        assertThat(response.sourceUrl()).isEqualTo(expectedUrl);
        assertThat(response.rawHtml()).isEqualTo("<html>list</html>");
    }

    @Test
    void fetchTagPage_whenLocaleIsInvalid_throwsInvalidInput() {
        // given
        String invalidLocale = "ko-kr/../../evil";

        // when, then
        assertThatThrownBy(() -> fetchService.fetchTagPage(invalidLocale))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(restTemplate);
    }
}
