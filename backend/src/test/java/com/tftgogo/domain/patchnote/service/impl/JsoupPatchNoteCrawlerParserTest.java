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
    void parseDetailPage_whenH3AndH4AreNested_preservesTargetAndStatContext() throws IOException {
        // given
        String body = """
                <div id="patch-notes-container">
                    <h2>Augments</h2>
                    <h3>Chaos Calling</h3>
                    <h4>Damage Amp</h4>
                    <ul>
                        <li>10%</li>
                        <li>15%</li>
                    </ul>
                    <h3>Items</h3>
                    <h4>Radiant Items</h4>
                    <ul>
                        <li>Attack Speed: 20% <span class="change-indicator">⇒</span> 25%</li>
                    </ul>
                </div>
                """;
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5-notes/",
                detailFixture(body)
        );

        // when
        PatchNoteCrawlDocument document = parser.parseDetailPage(fetchedPage, null, "ko-kr");

        // then
        assertThat(document.rows()).hasSize(3);
        PatchChangeCrawlRow firstRow = document.rows().get(0);
        assertThat(firstRow.headingPath()).isEqualTo("Augments > Chaos Calling > Damage Amp");
        assertThat(firstRow.groupTitle()).isEqualTo("Chaos Calling");
        assertThat(firstRow.rowText()).isEqualTo("Damage Amp: 10%");

        PatchChangeCrawlRow secondRow = document.rows().get(1);
        assertThat(secondRow.groupTitle()).isEqualTo("Chaos Calling");
        assertThat(secondRow.rowText()).isEqualTo("Damage Amp: 15%");

        PatchChangeCrawlRow itemRow = document.rows().get(2);
        assertThat(itemRow.headingPath()).isEqualTo("Augments > Items > Radiant Items");
        assertThat(itemRow.groupTitle()).isEqualTo("Radiant Items");
        assertThat(itemRow.rowText()).isEqualTo("Attack Speed: 20% ⇒ 25%");
        assertThat(itemRow.beforeText()).isEqualTo("Attack Speed: 20%");
        assertThat(itemRow.afterText()).isEqualTo("25%");
    }

    @Test
    void parseDetailPage_whenListItemsAreNested_usesParentItemAsTargetAndStatAsDetail() throws IOException {
        // given
        String body = """
                <div id="patch-notes-container">
                    <h2>Augments</h2>
                    <ul>
                        <li>Treasure Trove
                            <ul>
                                <li>Damage Amp
                                    <ul>
                                        <li>10%</li>
                                        <li>15%</li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
                """;
        PatchNoteCrawlFetchedPage fetchedPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5-notes/",
                detailFixture(body)
        );

        // when
        PatchNoteCrawlDocument document = parser.parseDetailPage(fetchedPage, null, "ko-kr");

        // then
        assertThat(document.rows()).hasSize(2);
        PatchChangeCrawlRow firstRow = document.rows().get(0);
        assertThat(firstRow.headingPath()).isEqualTo("Augments > Treasure Trove > Damage Amp");
        assertThat(firstRow.groupTitle()).isEqualTo("Treasure Trove");
        assertThat(firstRow.rowText()).isEqualTo("Damage Amp: 10%");

        PatchChangeCrawlRow secondRow = document.rows().get(1);
        assertThat(secondRow.groupTitle()).isEqualTo("Treasure Trove");
        assertThat(secondRow.rowText()).isEqualTo("Damage Amp: 15%");
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

    private String detailFixture(String richTextBody) throws IOException {
        return """
                <!doctype html>
                <html lang="ko">
                <body>
                <script id="__NEXT_DATA__" type="application/json">
                {
                  "props": {
                    "pageProps": {
                      "page": {
                        "analytics": {
                          "contentId": "tft-patch-17-5"
                        },
                        "blades": [
                          {
                            "type": "articleMasthead",
                            "title": "Teamfight Tactics Patch 17.5 Notes",
                            "publishDate": "2026-06-09T09:00:00+09:00",
                            "description": {
                              "body": "<p>Patch summary</p>"
                            },
                            "authors": [
                              {
                                "name": "Riot Prism"
                              }
                            ]
                          },
                          {
                            "type": "patchNotesRichText",
                            "richText": {
                              "type": "html",
                              "body": %s
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                </script>
                </body>
                </html>
                """.formatted(objectMapper.writeValueAsString(richTextBody));
    }
}
