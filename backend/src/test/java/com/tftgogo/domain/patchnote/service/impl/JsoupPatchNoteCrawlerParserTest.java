package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JsoupPatchNoteCrawlerParserTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private PatchNoteCrawlerProperties properties = new PatchNoteCrawlerProperties();

    @InjectMocks
    private JsoupPatchNoteCrawlerParser parser;

    @Test
    void parseListPage_extractsArticleCardGridItems() throws IOException {
        // given
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/",
                fixture("list-ko-kr-teamfight-tactics-patch-notes.html")
        );

        // when
        List<PatchNoteCrawlListItem> items = parser.parseListPage(fetchedPage);

        // then
        assertThat(items).hasSize(1);
        PatchNoteCrawlListItem item = items.get(0);
        assertThat(item.title()).isEqualTo("전략적 팀 전투 패치 17.2 노트");
        assertThat(item.publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 9, 0));
        assertThat(item.description()).isEqualTo("신규 세트 밸런스 조정과 버그 수정입니다.");
        assertThat(item.contentId()).isEqualTo("tft-patch-17-2");
        assertThat(item.detailUrl()).isEqualTo(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/"
        );
    }

    @Test
    void parseDetailPage_extractsMastheadAndRows() throws IOException {
        // given
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/",
                fixture("detail-ko-kr-17-2.html")
        );

        // when
        PatchNoteCrawlDocument document = parser.parseDetailPage(fetchedPage, null, "ko-kr");

        // then
        assertThat(document.title()).isEqualTo("전략적 팀 전투 패치 17.2 노트");
        assertThat(document.version()).isEqualTo("17.2");
        assertThat(document.summary()).isEqualTo("17.2 패치의 핵심 변경사항입니다.");
        assertThat(document.authors()).containsExactly("Riot Prism");
        assertThat(document.rows()).hasSize(3);

        PatchChangeCrawlRow firstRow = document.rows().get(0);
        assertThat(firstRow.headingPath()).isEqualTo("챔피언 > 징크스");
        assertThat(firstRow.sourceOrder()).isZero();
        assertThat(firstRow.rowText()).isEqualTo("징크스: 공격력 50 ⇒ 55");
        assertThat(firstRow.beforeText()).isEqualTo("징크스: 공격력 50");
        assertThat(firstRow.afterText()).isEqualTo("55");
        assertThat(firstRow.sourceKeyHash()).hasSize(64);
        assertThat(firstRow.sourceKeyCandidate()).contains("tft-patch-17-2");

        PatchChangeCrawlRow ambiguousRow = document.rows().get(1);
        assertThat(ambiguousRow.beforeText()).isNull();
        assertThat(ambiguousRow.afterText()).isNull();
        assertThat(ambiguousRow.parserWarnings()).contains("multiple change indicators");
    }

    @Test
    void parseDetailPage_defensivelyCopiesParserWarnings() throws IOException {
        // given
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/",
                fixture("detail-ko-kr-17-2.html")
        );

        // when
        PatchNoteCrawlDocument document = parser.parseDetailPage(fetchedPage, null, "ko-kr");

        // then
        assertThatThrownBy(() -> document.rows().get(1).parserWarnings().add("changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void parseDetailPage_whenNextDataMissing_throwsInvalidData() {
        // given
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/",
                "<html><body>No Next data</body></html>"
        );

        // when, then
        assertThatThrownBy(() -> parser.parseDetailPage(fetchedPage, null, "ko-kr"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
    }

    private PatchNoteCrawlFetchedPage fetchedPage(String sourceUrl, String rawHtml) {
        return new PatchNoteCrawlFetchedPage(sourceUrl, rawHtml, LocalDateTime.now(), 200);
    }

    private String fixture(String fileName) throws IOException {
        try (var inputStream = getClass().getClassLoader()
                .getResourceAsStream("patchnote/crawl/" + fileName)) {
            if (inputStream == null) {
                throw new IOException("Missing fixture: " + fileName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
