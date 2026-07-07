package com.tftgogo.domain.patchnote.scheduler;

import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchNoteImportSchedulerTest {

    @Mock
    private AdminPatchNoteService adminPatchNoteService;

    @Mock
    private PatchNoteCrawlerFetchService crawlerFetchService;

    @Mock
    private PatchNoteCrawlerParser crawlerParser;

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchNoteImportSchedulerLock schedulerLock;

    private PatchNoteImportSchedulerProperties properties;
    private PatchNoteImportScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new PatchNoteImportSchedulerProperties();
        scheduler = new PatchNoteImportScheduler(
                adminPatchNoteService,
                crawlerFetchService,
                crawlerParser,
                patchNoteRepository,
                properties,
                schedulerLock
        );
    }

    @Test
    void scheduler_disabled이면_목록_확인을_실행하지_않는다() {
        // when
        scheduler.importNewPatchNotesFromList();

        // then
        verifyNoInteractions(crawlerFetchService, crawlerParser, patchNoteRepository, adminPatchNoteService);
    }

    @Test
    void startup_import가_false이면_서버_시작_import를_실행하지_않는다() {
        // given
        properties.setEnabled(true);

        // when
        scheduler.importOnStartupIfEnabled();

        // then
        verifyNoInteractions(adminPatchNoteService);
    }

    @Test
    void startup_import는_이미_import된_최신_패치도_refresh한다() {
        // given
        properties.setEnabled(true);
        properties.setStartupImport(true);
        givenSchedulerLockRunsTask();
        PatchNoteCrawlListItem latestImported = listItem(
                "?꾨왂??? ?꾪닾 17.5 ?⑥튂",
                "riot-content-17-5",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5/"
        );
        givenPatchList(latestImported);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5"))
                .thenReturn(Optional.of(patchNote("17.5")));
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(importResponse("17.5", latestImported.detailUrl()));

        // when
        scheduler.importOnStartupIfEnabled();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService).importRiotPatchNote(captor.capture());
        assertThat(captor.getValue().getSourceUrl()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getLocale()).isEqualTo("ko-kr");
        assertThat(captor.getValue().shouldMarkCurrent()).isTrue();
    }

    @Test
    void daily_refresh_refreshes_latest_patch_note_even_if_already_imported() {
        // given
        properties.setEnabled(true);
        givenSchedulerLockRunsTask();
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(importResponse("17.5", "https://example.com/17-5"));

        // when
        scheduler.refreshLatestPatchNote();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService).importRiotPatchNote(captor.capture());
        assertThat(captor.getValue().getSourceUrl()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
        assertThat(captor.getValue().getLocale()).isEqualTo("ko-kr");
        assertThat(captor.getValue().shouldMarkCurrent()).isTrue();
    }

    @Test
    void list_check_skips_items_older_than_history_months() {
        // given
        properties.setEnabled(true);
        givenSchedulerLockRunsTask();
        PatchNoteCrawlListItem latestImported = listItem(
                "Teamfight Tactics patch 17.5",
                "riot-content-17-5",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5/"
        );
        PatchNoteCrawlListItem oldNew = listItem(
                "Teamfight Tactics patch 17.1",
                LocalDateTime.now().minusMonths(7),
                "riot-content-17-1",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-1/"
        );
        givenPatchList(latestImported, oldNew);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5"))
                .thenReturn(Optional.of(patchNote("17.5")));

        // when
        scheduler.importNewPatchNotesFromList();

        // then
        verifyNoInteractions(adminPatchNoteService);
    }

    @Test
    void list_check_skips_items_without_published_at() {
        // given
        properties.setEnabled(true);
        givenSchedulerLockRunsTask();
        PatchNoteCrawlListItem noDateNew = listItem(
                "Teamfight Tactics patch 17.5",
                null,
                "riot-content-17-5",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5/"
        );
        givenPatchList(noDateNew);

        // when
        scheduler.importNewPatchNotesFromList();

        // then
        verifyNoInteractions(adminPatchNoteService);
    }

    @Test
    void list_check_continues_after_single_item_import_failure() {
        // given
        properties.setEnabled(true);
        givenSchedulerLockRunsTask();
        PatchNoteCrawlListItem latestNew = listItem(
                "Teamfight Tactics patch 17.5",
                "riot-content-17-5",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5/"
        );
        PatchNoteCrawlListItem failingOld = listItem(
                "Teamfight Tactics patch 17.4",
                "riot-content-17-4",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-4/"
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
        scheduler.importNewPatchNotesFromList();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService, times(2)).importRiotPatchNote(captor.capture());
        assertThat(captor.getAllValues().get(0).getSourceUrl()).isEqualTo(failingOld.detailUrl());
        assertThat(captor.getAllValues().get(1).getSourceUrl()).isEqualTo(latestNew.detailUrl());
        assertThat(captor.getAllValues().get(1).shouldMarkCurrent()).isTrue();
    }

    @Test
    void 목록_확인은_DB에_없는_패치노트만_상세_import한다() {
        // given
        properties.setEnabled(true);
        givenSchedulerLockRunsTask();
        PatchNoteCrawlFetchedPage listPage = fetchedPage();
        PatchNoteCrawlListItem latestNew = listItem(
                "전략적 팀 전투 17.5 패치",
                "riot-content-17-5",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-5/"
        );
        PatchNoteCrawlListItem alreadyImported = listItem(
                "전략적 팀 전투 17.4 패치",
                "riot-content-17-4",
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-4/"
        );

        when(crawlerFetchService.fetchTagPage("ko-kr")).thenReturn(listPage);
        when(crawlerParser.parseListPage(listPage)).thenReturn(List.of(latestNew, alreadyImported));
        when(patchNoteRepository.findBySourceKey("riot-content-17-4"))
                .thenReturn(Optional.of(patchNote("17.4")));
        when(patchNoteRepository.findBySourceKey("riot-content-17-5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(latestNew.detailUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.5")).thenReturn(Optional.empty());
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenReturn(importResponse("17.5", latestNew.detailUrl()));

        // when
        scheduler.importNewPatchNotesFromList();

        // then
        ArgumentCaptor<AdminPatchNoteImportRequest> captor =
                ArgumentCaptor.forClass(AdminPatchNoteImportRequest.class);
        verify(adminPatchNoteService).importRiotPatchNote(captor.capture());
        assertThat(captor.getValue().getSourceUrl()).isEqualTo(latestNew.detailUrl());
        assertThat(captor.getValue().getVersion()).isEqualTo("17.5");
        assertThat(captor.getValue().shouldMarkCurrent()).isTrue();
    }

    @Test
    void 이미_import가_실행중이면_다른_스케줄은_skip한다() {
        // given
        properties.setEnabled(true);
        properties.setStartupImport(true);
        givenSchedulerLockRunsTask();
        givenPatchList();
        doAnswer(invocation -> {
            scheduler.refreshLatestPatchNote();
            return importResponse("17.5", "https://example.com/17-5");
        }).when(adminPatchNoteService).importRiotPatchNote(any(AdminPatchNoteImportRequest.class));

        // when
        scheduler.importOnStartupIfEnabled();

        // then
        verify(adminPatchNoteService, times(1)).importRiotPatchNote(any(AdminPatchNoteImportRequest.class));
    }

    @Test
    void 스케줄_import_실패는_서버_실행을_막지_않는다() {
        // given
        properties.setEnabled(true);
        properties.setStartupImport(true);
        givenSchedulerLockRunsTask();
        when(adminPatchNoteService.importRiotPatchNote(any(AdminPatchNoteImportRequest.class)))
                .thenThrow(new RuntimeException("riot unavailable"));

        // when, then
        assertThatCode(() -> scheduler.importOnStartupIfEnabled())
                .doesNotThrowAnyException();
    }

    @Test
    void DB_락을_얻지_못하면_import를_skip한다() {
        // given
        properties.setEnabled(true);
        properties.setStartupImport(true);
        when(schedulerLock.runWithLock(any(), any())).thenReturn(false);

        // when
        scheduler.importOnStartupIfEnabled();

        // then
        verifyNoInteractions(adminPatchNoteService);
    }

    private void givenSchedulerLockRunsTask() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return true;
        }).when(schedulerLock).runWithLock(any(), any());
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
