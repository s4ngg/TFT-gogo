package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.PatchNoteCrawlImportRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteCrawlImportResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.entity.PatchNoteImportSource;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchNoteCrawlerImportServiceImplTest {

    private static final String TAG_URL = "https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/";
    private static final String DETAIL_URL = "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2-notes/";

    @Mock
    private PatchNoteCrawlerFetchService fetchService;

    @Mock
    private PatchNoteCrawlerParser parser;

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchChangeRepository patchChangeRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private RecordingTransactionOperations transactionOperations;

    private PatchNoteCrawlerImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        transactionOperations = new RecordingTransactionOperations();
        importService = new PatchNoteCrawlerImportServiceImpl(
                fetchService,
                parser,
                patchNoteRepository,
                patchChangeRepository,
                new PatchNoteCrawlerProperties(),
                objectMapper,
                transactionOperations
        );
    }

    @Test
    void importPatchNote_whenDryRun_discoversLatestDetailAndDoesNotSave() {
        // given
        PatchNoteCrawlImportRequest request = request(null, null, null, null, null);
        PatchNoteCrawlFetchedPage tagPage = fetchedPage(TAG_URL);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = document();
        when(fetchService.fetchTagPage("ko-kr")).thenReturn(tagPage);
        when(parser.parseListPage(tagPage)).thenReturn(List.of(listItem()));
        when(fetchService.fetch(DETAIL_URL)).thenReturn(detailPage);
        when(parser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findByVersion("17.2")).thenReturn(Optional.empty());

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.isDryRun()).isTrue();
        assertThat(response.getSourceUrl()).isEqualTo(DETAIL_URL);
        assertThat(response.getVersion()).isEqualTo("17.2");
        assertThat(response.getCreatedCount()).isEqualTo(3);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getSkippedCount()).isZero();
        assertThat(response.getReviewRequiredCount()).isZero();
        verify(patchNoteRepository, never()).save(any(PatchNote.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
    }

    @Test
    void importPatchNote_whenWriteMode_createsPatchNoteAndChanges() {
        // given
        PatchNoteCrawlImportRequest request = request(DETAIL_URL, "17.2", "ko-kr", false, false);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = document();
        when(fetchService.fetch(DETAIL_URL)).thenReturn(detailPage);
        when(parser.parseDetailPage(detailPage, "17.2", "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findByVersion("17.2")).thenReturn(Optional.empty());
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 1L);
            return patchNote;
        });

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.isDryRun()).isFalse();
        assertThat(response.getPatchNoteId()).isEqualTo(1L);
        assertThat(response.getCreatedCount()).isEqualTo(3);

        ArgumentCaptor<PatchNote> patchNoteCaptor = ArgumentCaptor.forClass(PatchNote.class);
        verify(patchNoteRepository).save(patchNoteCaptor.capture());
        assertThat(patchNoteCaptor.getValue().getVersion()).isEqualTo("17.2");
        assertThat(patchNoteCaptor.getValue().getImportSource()).isEqualTo(PatchNoteImportSource.RIOT_OFFICIAL);
        verify(patchChangeRepository, times(2)).save(any(PatchChange.class));
    }

    @Test
    void importPatchNote_fetchesAndParsesOutsideTransaction_thenWritesInsideTransaction() {
        // given
        PatchNoteCrawlImportRequest request = request(DETAIL_URL, "17.2", "ko-kr", false, false);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = document();
        when(fetchService.fetch(DETAIL_URL)).thenAnswer(invocation -> {
            assertThat(transactionOperations.isInTransaction()).isFalse();
            return detailPage;
        });
        when(parser.parseDetailPage(detailPage, "17.2", "ko-kr")).thenAnswer(invocation -> {
            assertThat(transactionOperations.isInTransaction()).isFalse();
            return document;
        });
        when(patchNoteRepository.findByVersion("17.2")).thenAnswer(invocation -> {
            assertThat(transactionOperations.isInTransaction()).isTrue();
            return Optional.empty();
        });
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            assertThat(transactionOperations.isInTransaction()).isTrue();
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 1L);
            return patchNote;
        });
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            assertThat(transactionOperations.isInTransaction()).isTrue();
            return invocation.getArgument(0);
        });

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.getPatchNoteId()).isEqualTo(1L);
        verify(patchChangeRepository, times(2)).save(any(PatchChange.class));
    }

    @Test
    void importPatchNote_whenImportedRowsManuallyEdited_skipsWithoutForceOverwrite() {
        // given
        PatchNoteCrawlImportRequest request = request(DETAIL_URL, "17.2", "ko-kr", false, false);
        PatchNote existingPatchNote = importedPatchNote(true);
        PatchChange existingPatchChange = importedPatchChange(existingPatchNote, "source-key-1", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = documentWithSingleRow("source-key-1");
        when(fetchService.fetch(DETAIL_URL)).thenReturn(detailPage);
        when(parser.parseDetailPage(detailPage, "17.2", "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findByVersion("17.2")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "source-key-1"))
                .thenReturn(Optional.of(existingPatchChange));

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getSkippedCount()).isEqualTo(2);
        assertThat(existingPatchNote.getTitle()).isEqualTo("old title");
        assertThat(existingPatchChange.getSummary()).isEqualTo("old summary");
        verify(patchNoteRepository, never()).save(any(PatchNote.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
    }

    @Test
    void importPatchNote_whenForceOverwrite_updatesImportedManualRows() {
        // given
        PatchNoteCrawlImportRequest request = request(DETAIL_URL, "17.2", "ko-kr", false, true);
        PatchNote existingPatchNote = importedPatchNote(true);
        PatchChange existingPatchChange = importedPatchChange(existingPatchNote, "source-key-1", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = documentWithSingleRow("source-key-1");
        when(fetchService.fetch(DETAIL_URL)).thenReturn(detailPage);
        when(parser.parseDetailPage(detailPage, "17.2", "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findByVersion("17.2")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "source-key-1"))
                .thenReturn(Optional.of(existingPatchChange));

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.getUpdatedCount()).isEqualTo(2);
        assertThat(response.getSkippedCount()).isZero();
        assertThat(existingPatchNote.getTitle()).isEqualTo("전략적 팀 전투 패치 17.2 노트");
        assertThat(existingPatchChange.getSummary()).isEqualTo("징크스: 공격력 증가");
        verify(patchNoteRepository, never()).save(any(PatchNote.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
    }

    @Test
    void importPatchNote_whenParserReturnsDuplicateSourceKey_reportsRowErrorAndSkipsDuplicate() {
        // given
        PatchNoteCrawlImportRequest request = request(DETAIL_URL, "17.2", "ko-kr", true, false);
        PatchNote existingPatchNote = importedPatchNote(false);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(DETAIL_URL);
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                DETAIL_URL,
                "ko-kr",
                "content-id",
                "전략적 팀 전투 패치 17.2 노트",
                "17.2",
                "summary",
                LocalDateTime.of(2026, 6, 10, 9, 0),
                "https://example.com/banner.jpg",
                List.of("Riot Prism"),
                List.of("챔피언 > 징크스"),
                List.of(row("duplicate-key", "챔피언 > 징크스", 0), row("duplicate-key", "챔피언 > 징크스", 1)),
                List.of()
        );
        when(fetchService.fetch(DETAIL_URL)).thenReturn(detailPage);
        when(parser.parseDetailPage(detailPage, "17.2", "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findByVersion("17.2")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "duplicate-key"))
                .thenReturn(Optional.empty());

        // when
        PatchNoteCrawlImportResponse response = importService.importPatchNote(request);

        // then
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getRowErrors()).hasSize(1);
        assertThat(response.getCreatedCount()).isEqualTo(1);
        verify(patchChangeRepository).findByPatchNoteAndSourceKey(existingPatchNote, "duplicate-key");
    }

    private PatchNoteCrawlFetchedPage fetchedPage(String sourceUrl) {
        return new PatchNoteCrawlFetchedPage(
                sourceUrl,
                "<html></html>",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                200
        );
    }

    private PatchNoteCrawlListItem listItem() {
        return new PatchNoteCrawlListItem(
                "전략적 팀 전투 패치 17.2 노트",
                LocalDateTime.of(2026, 6, 10, 9, 0),
                "summary",
                "https://example.com/banner.jpg",
                "content-id",
                DETAIL_URL
        );
    }

    private PatchNoteCrawlDocument document() {
        return documentWithSingleRow("source-key-1", row("source-key-2", "아이템 > 구인수의 격노검", 1));
    }

    private PatchNoteCrawlDocument documentWithSingleRow(String sourceKey) {
        return documentWithSingleRow(sourceKey, null);
    }

    private PatchNoteCrawlDocument documentWithSingleRow(String sourceKey, PatchChangeCrawlRow additionalRow) {
        List<PatchChangeCrawlRow> rows = new java.util.ArrayList<>();
        rows.add(row(sourceKey, "챔피언 > 징크스", 0));
        if (additionalRow != null) {
            rows.add(additionalRow);
        }

        return new PatchNoteCrawlDocument(
                DETAIL_URL,
                "ko-kr",
                "content-id",
                "전략적 팀 전투 패치 17.2 노트",
                "17.2",
                "17.2 패치 핵심 요약",
                LocalDateTime.of(2026, 6, 10, 9, 0),
                "https://example.com/banner.jpg",
                List.of("Riot Prism"),
                List.of("챔피언 > 징크스"),
                rows,
                List.of()
        );
    }

    private PatchChangeCrawlRow row(String sourceKey, String headingPath, int sourceOrder) {
        return new PatchChangeCrawlRow(
                "candidate-" + sourceOrder,
                sourceKey,
                headingPath,
                sourceOrder,
                headingPath.split(" > ")[0],
                headingPath.split(" > ")[1],
                "징크스: 공격력 증가",
                "<li>징크스: 공격력 증가</li>",
                null,
                null,
                List.of()
        );
    }

    private PatchNote importedPatchNote(boolean manuallyEdited) {
        PatchNote patchNote = PatchNote.builder()
                .version("17.2")
                .title("old title")
                .summary("old summary")
                .description("old description")
                .sourceUrl(DETAIL_URL)
                .sourceLocale("ko-kr")
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .manuallyEdited(manuallyEdited)
                .publishedAt(LocalDateTime.of(2026, 6, 10, 9, 0))
                .current(false)
                .active(true)
                .build();
        ReflectionTestUtils.setField(patchNote, "id", 1L);
        return patchNote;
    }

    private PatchChange importedPatchChange(PatchNote patchNote, String sourceKey, boolean manuallyEdited) {
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .sourceKey(sourceKey)
                .sourceUrl(DETAIL_URL)
                .sourceHeadingPath("챔피언 > 징크스")
                .sourceOrder(0)
                .sourceLocale("ko-kr")
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .manuallyEdited(manuallyEdited)
                .category(PatchChangeCategory.CHAMPION)
                .changeType(PatchChangeType.BUFF)
                .impact(PatchChangeImpact.MEDIUM)
                .targetKey("champion-jinx")
                .targetName("Jinx")
                .summary("old summary")
                .sortOrder(0)
                .active(true)
                .build();
        ReflectionTestUtils.setField(patchChange, "id", 10L);
        return patchChange;
    }

    private PatchNoteCrawlImportRequest request(
            String sourceUrl,
            String version,
            String locale,
            Boolean dryRun,
            Boolean forceOverwrite) {
        PatchNoteCrawlImportRequest request = new PatchNoteCrawlImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", sourceUrl);
        ReflectionTestUtils.setField(request, "version", version);
        ReflectionTestUtils.setField(request, "locale", locale);
        ReflectionTestUtils.setField(request, "dryRun", dryRun);
        ReflectionTestUtils.setField(request, "forceOverwrite", forceOverwrite);
        return request;
    }

    private static class RecordingTransactionOperations implements TransactionOperations {

        private boolean inTransaction;

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            inTransaction = true;
            try {
                return action.doInTransaction(new SimpleTransactionStatus());
            } finally {
                inTransaction = false;
            }
        }

        private boolean isInTransaction() {
            return inTransaction;
        }
    }
}
