package com.tftgogo.domain.patchnote.scheduler;

import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask.HistoryBackfillResult;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask.PatchNoteRefreshResult;
import com.tftgogo.domain.patchnote.service.AdminPatchNoteService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchNoteImportTaskTest {

    @Mock
    private AdminPatchNoteService adminPatchNoteService;

    @Mock
    private PatchNoteCrawlerFetchService crawlerFetchService;

    @Mock
    private PatchNoteCrawlerParser crawlerParser;

    @Mock
    private PatchNoteRepository patchNoteRepository;

    private PatchNoteImportSchedulerProperties properties;
    private PatchNoteImportTask importTask;

    @BeforeEach
    void setUp() {
        properties = new PatchNoteImportSchedulerProperties();
        importTask = new PatchNoteImportTask(
                adminPatchNoteService,
                crawlerFetchService,
                crawlerParser,
                patchNoteRepository,
                properties
        );
    }

    @Test
    void 최신_패치노트를_현재_패치로_import한다() {
        // given
        properties.setLocale(" KO-KR ");
        AdminPatchNoteImportResponse response = importResponse("17.5", "https://example.com/17-5");
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(response);

        // when
        AdminPatchNoteImportResponse result = importTask.importLatestPatchNote();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService).importRiotPatchNote(captor.capture());
        assertThat(result).isSameAs(response);
        assertThat(captor.getValue().getSourceUrl()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getLocale()).isEqualTo("ko-kr");
        assertThat(captor.getValue().shouldMarkCurrent()).isTrue();
    }

    @Test
    void 최신_패치_commit_후_history_backfill이_실패해도_최신_결과를_반환한다() {
        // given
        AdminPatchNoteImportResponse response = importResponse("17.5", "https://example.com/17-5");
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(response);
        when(crawlerFetchService.fetchTagPage("ko-kr"))
                .thenThrow(new RuntimeException("riot list unavailable"));

        // when
        PatchNoteRefreshResult result = importTask.importLatestPatchNoteThenUnknownPatchNotesFromList();

        // then
        assertThat(result.latestPatchNote()).isSameAs(response);
        assertThat(result.hasHistoryFailures()).isTrue();
        assertThat(result.historyBackfill().failedCount()).isEqualTo(1);
        verify(adminPatchNoteService).importRiotPatchNote(any(AdminPatchNoteImportRequest.class));
    }

    @Test
    void history_기간보다_오래됐거나_게시일이_없는_패치는_skip한다() {
        // given
        PatchNoteCrawlListItem oldItem = listItem(
                "Teamfight Tactics patch 17.1",
                LocalDateTime.now().minusMonths(7),
                "riot-content-17-1",
                "https://example.com/teamfight-tactics-patch-17-1/"
        );
        PatchNoteCrawlListItem noDateItem = listItem(
                "Teamfight Tactics patch 17.2",
                null,
                "riot-content-17-2",
                "https://example.com/teamfight-tactics-patch-17-2/"
        );
        givenPatchList(oldItem, noDateItem);

        // when
        HistoryBackfillResult result = importTask.importUnknownPatchNotesFromList();

        // then
        verifyNoInteractions(patchNoteRepository, adminPatchNoteService);
        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.importedCount()).isZero();
        assertThat(result.failedCount()).isZero();
    }

    @Test
    void history는_오래된_항목부터_import하고_한_항목_실패후에도_계속한다() {
        // given
        PatchNoteCrawlListItem latestNew = listItem(
                "Teamfight Tactics patch 17.5",
                "riot-content-17-5",
                "https://example.com/teamfight-tactics-patch-17-5/"
        );
        PatchNoteCrawlListItem failingOld = listItem(
                "Teamfight Tactics patch 17.4",
                "riot-content-17-4",
                "https://example.com/teamfight-tactics-patch-17-4/"
        );
        givenPatchList(latestNew, failingOld);
        givenPatchNoteIsNotImported(latestNew, "17.5");
        givenPatchNoteIsNotImported(failingOld, "17.4");
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenAnswer(invocation -> {
                    AdminPatchNoteImportRequest request = invocation.getArgument(0);
                    if (request.getSourceUrl().contains("17-4")) {
                        throw new RuntimeException("riot unavailable");
                    }
                    return importResponse("17.5", latestNew.detailUrl());
                });

        // when
        HistoryBackfillResult result = importTask.importUnknownPatchNotesFromList();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService, times(2)).importRiotPatchNote(captor.capture());
        List<AdminPatchNoteImportRequest> requests = captor.getAllValues();
        assertThat(requests.get(0).getSourceUrl()).isEqualTo(failingOld.detailUrl());
        assertThat(requests.get(0).shouldMarkCurrent()).isFalse();
        assertThat(requests.get(1).getSourceUrl()).isEqualTo(latestNew.detailUrl());
        assertThat(requests.get(1).shouldMarkCurrent()).isTrue();
        assertThat(result.scannedCount()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void history에서_DB에_없는_패치만_import한다() {
        // given
        PatchNoteCrawlListItem latestNew = listItem(
                "전략적 팀 전투 17.5 패치",
                "riot-content-17-5",
                "https://example.com/teamfight-tactics-patch-17-5/"
        );
        PatchNoteCrawlListItem alreadyImported = listItem(
                "전략적 팀 전투 17.4 패치",
                "riot-content-17-4",
                "https://example.com/teamfight-tactics-patch-17-4/"
        );
        givenPatchList(latestNew, alreadyImported);
        when(patchNoteRepository.findBySourceKey("riot-content-17-4"))
                .thenReturn(Optional.of(patchNote("17.4")));
        givenPatchNoteIsNotImported(latestNew, "17.5");
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(importResponse("17.5", latestNew.detailUrl()));

        // when
        importTask.importUnknownPatchNotesFromList();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService).importRiotPatchNote(captor.capture());
        assertThat(captor.getValue().getSourceUrl()).isEqualTo(latestNew.detailUrl());
        assertThat(captor.getValue().getVersion()).isEqualTo("17.5");
        assertThat(captor.getValue().shouldMarkCurrent()).isTrue();
    }

    private void givenPatchList(PatchNoteCrawlListItem... items) {
        PatchNoteCrawlFetchedPage listPage = fetchedPage();
        when(crawlerFetchService.fetchTagPage("ko-kr")).thenReturn(listPage);
        when(crawlerParser.parseListPage(listPage)).thenReturn(List.of(items));
    }

    private void givenPatchNoteIsNotImported(PatchNoteCrawlListItem item, String version) {
        when(patchNoteRepository.findBySourceKey(item.contentId())).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(item.detailUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion(version)).thenReturn(Optional.empty());
    }

    private PatchNoteCrawlFetchedPage fetchedPage() {
        return new PatchNoteCrawlFetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/tags/patch-notes/",
                "<html></html>",
                LocalDateTime.of(2026, 6, 18, 9, 0),
                200
        );
    }

    private PatchNoteCrawlListItem listItem(String title, String contentId, String detailUrl) {
        return listItem(title, LocalDateTime.now().minusDays(1), contentId, detailUrl);
    }

    private PatchNoteCrawlListItem listItem(
            String title,
            LocalDateTime publishedAt,
            String contentId,
            String detailUrl
    ) {
        return new PatchNoteCrawlListItem(
                title,
                publishedAt,
                "summary",
                "",
                contentId,
                detailUrl
        );
    }

    private PatchNote patchNote(String version) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .publishedAt(LocalDateTime.of(2026, 6, 18, 9, 0))
                .current(false)
                .build();
        ReflectionTestUtils.setField(patchNote, "id", 1L);
        return patchNote;
    }

    private AdminPatchNoteImportResponse importResponse(String version, String sourceUrl) {
        return AdminPatchNoteImportResponse.of(
                1L,
                version,
                sourceUrl,
                true,
                false,
                false,
                1,
                0,
                0,
                List.of()
        );
    }
}
