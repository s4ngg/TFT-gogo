package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
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
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPatchNoteServiceImplTest {

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchChangeRepository patchChangeRepository;

    @Mock
    private GuideChampionRepository guideChampionRepository;

    @Mock
    private GuideTraitRepository guideTraitRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PatchNoteCrawlerFetchService crawlerFetchService;

    @Mock
    private PatchNoteCrawlerParser crawlerParser;

    @Mock
    private PatchNoteCrawlerProperties crawlerProperties;

    @InjectMocks
    private AdminPatchNoteServiceImpl adminPatchNoteService;

    @Test
    void createPatchNote_whenCurrentTrue_clearsExistingCurrentPatch() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.3", true);
        AdminPatchNoteRequest request = patchNoteRequest("17.4", true, List.of("current patch"));
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull())
                .thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });

        // when
        PatchNoteResponse response = adminPatchNoteService.createPatchNote(request);

        // then
        assertThat(existingCurrent.isCurrent()).isFalse();
        assertThat(response.getVersion()).isEqualTo("17.4");
        assertThat(response.getIsCurrent()).isTrue();
        assertThat(response.getHighlights()).containsExactly("current patch");

        ArgumentCaptor<PatchNote> captor = ArgumentCaptor.forClass(PatchNote.class);
        InOrder inOrder = inOrder(patchNoteRepository);
        inOrder.verify(patchNoteRepository).flush();
        inOrder.verify(patchNoteRepository).save(captor.capture());
        assertThat(captor.getValue().getHighlightsJson()).isEqualTo("[\"current patch\"]");
    }

    @Test
    void createPatchNote_whenDescriptionBlank_usesSummaryAsContent() {
        // given
        AdminPatchNoteRequest request = patchNoteRequest("17.4", false, List.of());
        ReflectionTestUtils.setField(request, "description", " ");
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.empty());
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });

        // when
        adminPatchNoteService.createPatchNote(request);

        // then
        ArgumentCaptor<PatchNote> captor = ArgumentCaptor.forClass(PatchNote.class);
        verify(patchNoteRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("summary");
    }

    @Test
    void importRiotPatchNote_whenRequestIsEmpty_importsLatestPatchFromTagPage() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.3", true);
        PatchNoteCrawlFetchedPage listPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/");
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-4-notes/");
        PatchNoteCrawlListItem listItem = new PatchNoteCrawlListItem(
                "17.4 Patch Notes",
                LocalDateTime.of(2026, 6, 15, 9, 0),
                "summary",
                "https://example.com/patch.png",
                "riot-content-17-4",
                detailPage.sourceUrl()
        );
        PatchChangeCrawlRow row = new PatchChangeCrawlRow(
                "candidate",
                "source-row-key",
                "Champions > Jinx",
                3,
                "Champions",
                "Jinx",
                "Attack damage increased",
                "<li>Attack damage increased</li>",
                "50",
                "55",
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-4",
                "17.4 Patch Notes",
                "17.4",
                "Riot summary",
                LocalDateTime.of(2026, 6, 15, 9, 0),
                "https://example.com/patch.png",
                List.of("Riot"),
                List.of("Champions > Jinx", "Traits > (6) Animal Squad"),
                List.of(row),
                List.of("parser warning")
        );

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetchTagPage("ko-kr")).thenReturn(listPage);
        when(crawlerParser.parseListPage(listPage)).thenReturn(List.of(listItem));
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-4")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()).thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("source-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(new AdminPatchNoteImportRequest());

        // then
        assertThat(existingCurrent.isCurrent()).isFalse();
        assertThat(response.isPatchNoteCreated()).isTrue();
        assertThat(response.getCreatedChanges()).isEqualTo(1);
        assertThat(response.getParserWarnings()).containsExactly("parser warning");

        ArgumentCaptor<PatchNote> patchNoteCaptor = ArgumentCaptor.forClass(PatchNote.class);
        InOrder inOrder = inOrder(patchNoteRepository);
        inOrder.verify(patchNoteRepository).flush();
        inOrder.verify(patchNoteRepository).save(patchNoteCaptor.capture());
        assertThat(patchNoteCaptor.getValue().getSourceKey()).isEqualTo("riot-content-17-4");
        assertThat(patchNoteCaptor.getValue().getImportSource()).isEqualTo(PatchNoteImportSource.RIOT_OFFICIAL);
        assertThat(patchNoteCaptor.getValue().getFocus()).isEqualTo("Riot summary");
        assertThat(patchNoteCaptor.getValue().getHighlightsJson()).isEqualTo("[\"Jinx\",\"Animal Squad\"]");
        assertThat(patchNoteCaptor.getValue().isCurrent()).isTrue();

        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository).save(patchChangeCaptor.capture());
        assertThat(patchChangeCaptor.getValue().getSourceKey()).isEqualTo("source-row-key");
        assertThat(patchChangeCaptor.getValue().getSourceHeadingPath()).isEqualTo("Champions > Jinx");
        assertThat(patchChangeCaptor.getValue().getSourceOrder()).isEqualTo(3);
        assertThat(patchChangeCaptor.getValue().getCategory()).isEqualTo(PatchChangeCategory.CHAMPION);
        assertThat(patchChangeCaptor.getValue().getChangeType()).isEqualTo(PatchChangeType.ADJUST);
        assertThat(patchChangeCaptor.getValue().getTagsJson()).isNull();
    }

    @Test
    void importRiotPatchNote_whenNewKeywordExists_savesAsNew() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.4", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-5-notes/");
        PatchChangeCrawlRow row = new PatchChangeCrawlRow(
                "new-candidate",
                "new-row-key",
                "시스템",
                1,
                "시스템",
                "",
                "신규 조우자가 추가됩니다.",
                "<li>신규 조우자가 추가됩니다.</li>",
                null,
                null,
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-5-new",
                "17.5 Patch Notes",
                "17.5",
                "Official summary",
                LocalDateTime.of(2026, 6, 9, 18, 0),
                "https://example.com/official.png",
                List.of("Riot"),
                List.of("시스템"),
                List.of(row),
                List.of()
        );
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", detailPage.sourceUrl());

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5-new")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()).thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("new-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getCreatedChanges()).isEqualTo(1);

        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository).save(patchChangeCaptor.capture());
        assertThat(patchChangeCaptor.getValue().getChangeType()).isEqualTo(PatchChangeType.NEW);
    }

    @Test
    void importRiotPatchNote_whenBugFixMentionsIncrease_savesAsSystemAdjust() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.3", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-5-notes/");
        PatchChangeCrawlRow row = new PatchChangeCrawlRow(
                "bug-candidate",
                "bug-row-key",
                "버그 수정",
                1,
                "버그 수정",
                "",
                "사미라의 처형타 확률 증가에 따른 상호작용이 잘못 적용되던 버그를 수정했습니다.",
                "<li>사미라의 처형타 확률 증가에 따른 상호작용이 잘못 적용되던 버그를 수정했습니다.</li>",
                null,
                null,
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-5",
                "17.5 Patch Notes",
                "17.5",
                "Official summary",
                LocalDateTime.of(2026, 6, 9, 18, 0),
                "https://example.com/official.png",
                List.of("Riot"),
                List.of("버그 수정"),
                List.of(row),
                List.of()
        );
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", detailPage.sourceUrl());

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()).thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("bug-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getCreatedChanges()).isEqualTo(1);

        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository).save(patchChangeCaptor.capture());
        assertThat(patchChangeCaptor.getValue().getCategory()).isEqualTo(PatchChangeCategory.SYSTEM);
        assertThat(patchChangeCaptor.getValue().getChangeType()).isEqualTo(PatchChangeType.ADJUST);
    }

    @Test
    void importRiotPatchNote_whenGuideNameAppearsWithoutCategoryHeading_infersGuideCategory() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.4", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-5-notes/");
        PatchChangeCrawlRow championRow = new PatchChangeCrawlRow(
                "twisted-fate-candidate",
                "twisted-fate-row-key",
                "변경사항",
                1,
                "변경사항",
                "트위스티드 페이트",
                "트위스티드 페이트 마나 조정",
                "<li>트위스티드 페이트 마나 조정</li>",
                "50",
                "45",
                List.of()
        );
        PatchChangeCrawlRow traitRow = new PatchChangeCrawlRow(
                "pathfinder-candidate",
                "pathfinder-row-key",
                "변경사항",
                2,
                "변경사항",
                "길잡이 뾰족꼬리 휩쓸기 피해량",
                "길잡이 체력 배수 (스테이지 4~6) 조정",
                "<li>길잡이 체력 배수 (스테이지 4~6) 조정</li>",
                "100%",
                "110%",
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-5-guide-name",
                "17.5 Patch Notes",
                "17.5",
                "Official summary",
                LocalDateTime.of(2026, 6, 9, 18, 0),
                "https://example.com/official.png",
                List.of("Riot"),
                List.of("변경사항"),
                List.of(championRow, traitRow),
                List.of()
        );
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", detailPage.sourceUrl());

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5-guide-name")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()).thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.5"))
                .thenReturn(List.of(guideChampion("tft17_twistedfate", "트위스티드 페이트", "17.5")));
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.5"))
                .thenReturn(List.of(guideTrait("tft17_pathfinder", "길잡이", "17.5")));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("twisted-fate-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("pathfinder-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getCreatedChanges()).isEqualTo(2);

        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository, times(2)).save(patchChangeCaptor.capture());
        assertThat(patchChangeCaptor.getAllValues())
                .extracting(PatchChange::getCategory)
                .containsExactly(PatchChangeCategory.CHAMPION, PatchChangeCategory.TRAIT);
        assertThat(patchChangeCaptor.getAllValues())
                .extracting(PatchChange::getTargetName)
                .containsExactly("트위스티드 페이트", "길잡이 뾰족꼬리 휩쓸기 피해량");
    }

    @Test
    void importRiotPatchNote_whenGuideNameIsEmbeddedInsideAnotherToken_keepsSystemCategory() {
        // given
        PatchNote existingCurrent = patchNote(1L, "17.4", true);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-5-notes/");
        PatchChangeCrawlRow row = new PatchChangeCrawlRow(
                "scouting-candidate",
                "scouting-row-key",
                "Balance",
                1,
                "Balance",
                "Scouting report",
                "Scouting report adjusted",
                "<li>Scouting report adjusted</li>",
                null,
                null,
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-5-guide-name-boundary",
                "17.5 Patch Notes",
                "17.5",
                "Official summary",
                LocalDateTime.of(2026, 6, 9, 18, 0),
                "https://example.com/official.png",
                List.of("Riot"),
                List.of("Balance"),
                List.of(row),
                List.of()
        );
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", detailPage.sourceUrl());

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-5-guide-name-boundary")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.5")).thenReturn(Optional.empty());
        when(patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()).thenReturn(List.of(existingCurrent));
        when(patchNoteRepository.save(any(PatchNote.class))).thenAnswer(invocation -> {
            PatchNote patchNote = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchNote, "id", 2L);
            return patchNote;
        });
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.5")).thenReturn(List.of());
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.5"))
                .thenReturn(List.of(guideTrait("tft17_scout", "Scout", "17.5")));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(any(PatchNote.class), eq("scouting-row-key")))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getCreatedChanges()).isEqualTo(1);

        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository).save(patchChangeCaptor.capture());
        assertThat(patchChangeCaptor.getValue().getCategory()).isEqualTo(PatchChangeCategory.SYSTEM);
    }

    @Test
    void importRiotPatchNote_whenExistingPatchFound_updatesOfficialMetadataAndDeletesOnlyImportedStaleChanges() {
        // given
        PatchNote existingPatchNote = patchNote(1L, "17.3", true);
        PatchChange staleChange = importedPatchChange(99L, existingPatchNote);
        PatchChange manualChange = patchChange(100L, existingPatchNote);
        PatchChange manuallyEditedImportedChange = importedPatchChange(
                101L,
                existingPatchNote,
                "manually-edited-imported-row",
                100
        );
        manuallyEditedImportedChange.markManuallyEditedIfImported();
        PatchNoteCrawlFetchedPage detailPage = fetchedPage("https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-3/");
        LocalDateTime officialPublishedAt = LocalDateTime.of(2026, 5, 12, 18, 0);
        PatchChangeCrawlRow row = new PatchChangeCrawlRow(
                "official-candidate",
                "official-row-key",
                "Augments",
                1,
                "Augments",
                "",
                "Augment balance adjusted",
                "<li>Augment balance adjusted</li>",
                null,
                null,
                List.of()
        );
        PatchNoteCrawlDocument document = new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-3",
                "17.3 Patch Notes",
                "17.3",
                "Official summary",
                officialPublishedAt,
                "https://example.com/official.png",
                List.of("Riot"),
                List.of("Augments"),
                List.of(row),
                List.of()
        );
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", detailPage.sourceUrl());
        ReflectionTestUtils.setField(request, "current", false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.empty());
        when(patchNoteRepository.findBySourceUrl(detailPage.sourceUrl())).thenReturn(Optional.empty());
        when(patchNoteRepository.findByVersion("17.3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "official-row-key"))
                .thenReturn(Optional.empty());
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(List.of(staleChange, manualChange, manuallyEditedImportedChange));

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.isPatchNoteUpdated()).isTrue();
        assertThat(response.getCreatedChanges()).isEqualTo(1);
        assertThat(existingPatchNote.getTitle()).isEqualTo("17.3 Patch Notes");
        assertThat(existingPatchNote.getPublishedAt()).isEqualTo(officialPublishedAt);
        assertThat(existingPatchNote.getFocus()).isEqualTo("Official summary");
        assertThat(existingPatchNote.getHighlightsJson()).isEqualTo("[\"Augments\"]");
        assertThat(existingPatchNote.isCurrent()).isFalse();
        assertThat(existingPatchNote.getSourceUrl()).isEqualTo(detailPage.sourceUrl());
        verify(patchChangeRepository).deleteAllInBatch(List.of(staleChange));
    }

    @Test
    void importRiotPatchNote_whenRowsAreEmpty_rejectsBeforeChangingData() {
        // given
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-3/"
        );
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(detailPage, List.of(), List.of());
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.importRiotPatchNote(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
        verify(patchNoteRepository, never()).save(any(PatchNote.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenFatalParserWarningExists_rejectsBeforeChangingData() {
        // given
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(
                "https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-3/"
        );
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(
                detailPage,
                List.of(patchChangeCrawlRow("row-1", 1)),
                List.of("max detail rows reached")
        );
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.importRiotPatchNote(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
        verify(patchNoteRepository, never()).save(any(PatchNote.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenRowsDropBelowMinimumRatio_preservesExistingData() {
        // given
        PatchNote existingPatchNote = importedPatchNote(1L, "17.3", true);
        List<PatchChange> existingChanges = List.of(
                importedPatchChange(1L, existingPatchNote, "row-1", 1),
                importedPatchChange(2L, existingPatchNote, "row-2", 2),
                importedPatchChange(3L, existingPatchNote, "row-3", 3),
                importedPatchChange(4L, existingPatchNote, "row-4", 4),
                importedPatchChange(5L, existingPatchNote, "row-5", 5)
        );
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(existingPatchNote.getSourceUrl());
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(
                detailPage,
                List.of(patchChangeCrawlRow("row-1", 1)),
                List.of()
        );
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerProperties.getMinRetainedRowRatio()).thenReturn(0.5);
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(existingChanges);

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.importRiotPatchNote(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
        assertThat(existingPatchNote.getTitle()).isEqualTo("17.3 patch");
        assertThat(existingPatchNote.isCurrent()).isTrue();
        verify(patchChangeRepository, never()).findByPatchNoteAndSourceKey(any(PatchNote.class), any(String.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenHeaderIsManuallyEditedAndRowsDropBelowMinimumRatio_preservesExistingData() {
        // given
        PatchNote existingPatchNote = importedPatchNote(1L, "17.3", true);
        existingPatchNote.markManuallyEditedIfImported();
        List<PatchChange> existingChanges = List.of(
                importedPatchChange(1L, existingPatchNote, "row-1", 1),
                importedPatchChange(2L, existingPatchNote, "row-2", 2),
                importedPatchChange(3L, existingPatchNote, "row-3", 3),
                importedPatchChange(4L, existingPatchNote, "row-4", 4),
                importedPatchChange(5L, existingPatchNote, "row-5", 5)
        );
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(existingPatchNote.getSourceUrl());
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(
                detailPage,
                List.of(patchChangeCrawlRow("row-1", 1)),
                List.of()
        );
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerProperties.getMinRetainedRowRatio()).thenReturn(0.5);
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(existingChanges);

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.importRiotPatchNote(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
        verify(patchChangeRepository, never()).findByPatchNoteAndSourceKey(any(PatchNote.class), any(String.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenRowCountIsSameButSourceKeysDoNotOverlap_preservesExistingData() {
        // given
        PatchNote existingPatchNote = importedPatchNote(1L, "17.3", true);
        List<PatchChange> existingChanges = List.of(
                importedPatchChange(1L, existingPatchNote, "existing-row-1", 1),
                importedPatchChange(2L, existingPatchNote, "existing-row-2", 2)
        );
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(existingPatchNote.getSourceUrl());
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(
                detailPage,
                List.of(
                        patchChangeCrawlRow("incoming-row-1", 1),
                        patchChangeCrawlRow("incoming-row-2", 2)
                ),
                List.of()
        );
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerProperties.getMinRetainedRowRatio()).thenReturn(0.5);
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(existingChanges);

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.importRiotPatchNote(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PATCH_NOTE_INVALID_DATA));
        verify(patchChangeRepository, never()).findByPatchNoteAndSourceKey(any(PatchNote.class), any(String.class));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenImportedRowsAreManuallyEdited_excludesThemFromRetainedRatio() {
        // given
        PatchNote existingPatchNote = importedPatchNote(1L, "17.3", true);
        PatchChange retainedChange = importedPatchChange(1L, existingPatchNote, "row-1", 1);
        PatchChange manuallyEditedChange = importedPatchChange(2L, existingPatchNote, "manual-row", 2);
        manuallyEditedChange.markManuallyEditedIfImported();
        List<PatchChange> existingChanges = List.of(retainedChange, manuallyEditedChange);
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(existingPatchNote.getSourceUrl());
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(
                detailPage,
                List.of(patchChangeCrawlRow("row-1", 1)),
                List.of()
        );
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerProperties.getMinRetainedRowRatio()).thenReturn(1.0);
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(existingChanges);
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "row-1"))
                .thenReturn(Optional.of(retainedChange));

        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getUpdatedChanges()).isEqualTo(1);
        verify(patchChangeRepository, never()).deleteAllInBatch(any());
    }

    @Test
    void importRiotPatchNote_whenRowRatioEqualsMinimum_updatesAndDeletesOnlyStaleRows() {
        // given
        PatchNote existingPatchNote = importedPatchNote(1L, "17.3", true);
        PatchChange first = importedPatchChange(1L, existingPatchNote, "row-1", 1);
        PatchChange second = importedPatchChange(2L, existingPatchNote, "row-2", 2);
        PatchChange firstStale = importedPatchChange(3L, existingPatchNote, "row-3", 3);
        PatchChange secondStale = importedPatchChange(4L, existingPatchNote, "row-4", 4);
        List<PatchChange> existingChanges = List.of(first, second, firstStale, secondStale);
        List<PatchChangeCrawlRow> rows = List.of(
                patchChangeCrawlRow("row-1", 1),
                patchChangeCrawlRow("row-2", 2)
        );
        PatchNoteCrawlFetchedPage detailPage = fetchedPage(existingPatchNote.getSourceUrl());
        PatchNoteCrawlDocument document = patchNoteCrawlDocument(detailPage, rows, List.of());
        AdminPatchNoteImportRequest request = patchNoteImportRequest(detailPage.sourceUrl(), false);

        when(crawlerProperties.getDefaultLocale()).thenReturn("ko-kr");
        when(crawlerProperties.getMinRetainedRowRatio()).thenReturn(0.5);
        when(crawlerFetchService.fetch(detailPage.sourceUrl())).thenReturn(detailPage);
        when(crawlerParser.parseDetailPage(detailPage, null, "ko-kr")).thenReturn(document);
        when(patchNoteRepository.findBySourceKey("riot-content-17-3")).thenReturn(Optional.of(existingPatchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(existingPatchNote))
                .thenReturn(existingChanges);
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "row-1"))
                .thenReturn(Optional.of(first));
        when(patchChangeRepository.findByPatchNoteAndSourceKey(existingPatchNote, "row-2"))
                .thenReturn(Optional.of(second));
        // when
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);

        // then
        assertThat(response.getUpdatedChanges()).isEqualTo(2);
        assertThat(existingPatchNote.isCurrent()).isFalse();
        verify(patchChangeRepository).deleteAllInBatch(List.of(firstStale, secondStale));
    }

    @Test
    void updatePatchNote_whenVersionAlreadyExists_throwsInvalidInput() {
        // given
        PatchNote target = patchNote(1L, "17.3", true);
        PatchNote duplicate = patchNote(2L, "17.4", false);
        AdminPatchNoteRequest request = patchNoteRequest("17.4", false, List.of());
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(target));
        when(patchNoteRepository.findByVersion("17.4")).thenReturn(Optional.of(duplicate));

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.updatePatchNote(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void updatePatchNote_whenImported_marksManuallyEdited() {
        // given
        PatchNote patchNote = importedPatchNote(1L, "17.3", true);
        AdminPatchNoteRequest request = patchNoteRequest("17.3", true, List.of("manual highlight"));
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchNoteRepository.findByVersion("17.3")).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.countByPatchNote(patchNote)).thenReturn(0L);

        // when
        PatchNoteResponse response = adminPatchNoteService.updatePatchNote(1L, request);

        // then
        assertThat(response.getHighlights()).containsExactly("manual highlight");
        assertThat(patchNote.isManuallyEdited()).isTrue();
    }

    @Test
    void deletePatchNote_softDeletesPatchNoteAndHardDeletesChanges() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange patchChange = patchChange(10L, patchNote);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(patchChange));

        // when
        adminPatchNoteService.deletePatchNote(1L);

        // then
        assertThat(patchNote.isCurrent()).isFalse();
        assertThat(patchNote.getDeletedAt()).isNotNull();
        verify(patchChangeRepository).delete(patchChange);
    }

    @Test
    void deletePatchNote_whenImported_marksManuallyEditedAndHardDeletesChanges() {
        // given
        PatchNote patchNote = importedPatchNote(1L, "17.3", true);
        PatchChange patchChange = importedPatchChange(10L, patchNote);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(patchChange));

        // when
        adminPatchNoteService.deletePatchNote(1L);

        // then
        assertThat(patchNote.isManuallyEdited()).isTrue();
        assertThat(patchChange.isManuallyEdited()).isTrue();
        assertThat(patchNote.getDeletedAt()).isNotNull();
        verify(patchChangeRepository).delete(patchChange);
    }

    @Test
    void getPatchChanges_returnsNonDeletedChangesForAdmin() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange firstChange = patchChange(10L, patchNote, 1);
        PatchChange hiddenChange = patchChange(11L, patchNote, 2);
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote))
                .thenReturn(List.of(firstChange, hiddenChange));

        // when
        List<PatchChangeResponse> responses = adminPatchNoteService.getPatchChanges(1L);

        // then
        assertThat(responses).extracting(PatchChangeResponse::getId).containsExactly(10L, 11L);
        assertThat(responses).extracting(PatchChangeResponse::getSortOrder).containsExactly(1, 2);
        verify(patchChangeRepository).findByPatchNoteOrderBySortOrderAscIdAsc(patchNote);
    }

    @Test
    void createPatchChange_marksManuallyEdited() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        AdminPatchChangeRequest request = patchChangeRequest(1L, "CHAMPION");
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));
        when(patchChangeRepository.save(any(PatchChange.class))).thenAnswer(invocation -> {
            PatchChange patchChange = invocation.getArgument(0);
            ReflectionTestUtils.setField(patchChange, "id", 10L);
            return patchChange;
        });

        // when
        PatchChangeResponse response = adminPatchNoteService.createPatchChange(request);

        // then
        ArgumentCaptor<PatchChange> patchChangeCaptor = ArgumentCaptor.forClass(PatchChange.class);
        verify(patchChangeRepository).save(patchChangeCaptor.capture());
        assertThat(response.getTargetName()).isEqualTo("Jinx");
        assertThat(patchChangeCaptor.getValue().isManuallyEdited()).isTrue();
    }

    @Test
    void createPatchChange_whenCategoryInvalid_throwsInvalidInput() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        AdminPatchChangeRequest request = patchChangeRequest(1L, "UNKNOWN");
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));

        // when, then
        assertThatThrownBy(() -> adminPatchNoteService.createPatchChange(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(patchChangeRepository, never()).save(any(PatchChange.class));
    }

    @Test
    void updatePatchChange_whenImported_marksManuallyEdited() {
        // given
        PatchNote patchNote = patchNote(1L, "17.3", true);
        PatchChange patchChange = importedPatchChange(10L, patchNote);
        AdminPatchChangeRequest request = patchChangeRequest(1L, "CHAMPION");
        when(patchChangeRepository.findById(10L)).thenReturn(Optional.of(patchChange));
        when(patchNoteRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(patchNote));

        // when
        PatchChangeResponse response = adminPatchNoteService.updatePatchChange(10L, request);

        // then
        assertThat(response.getTargetName()).isEqualTo("Jinx");
        assertThat(patchChange.isManuallyEdited()).isTrue();
    }

    private PatchNote patchNote(Long id, String version, boolean current) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .focus("balance")
                .imageUrl("https://example.com/patch.png")
                .publishedAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .current(current)
                .highlightsJson("[\"highlight\"]")
                .build();
        ReflectionTestUtils.setField(patchNote, "id", id);
        return patchNote;
    }

    private PatchNote importedPatchNote(Long id, String version, boolean current) {
        PatchNote patchNote = PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .focus("balance")
                .imageUrl("https://example.com/patch.png")
                .sourceKey("riot-content-17-3")
                .sourceUrl("https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-17-3-notes/")
                .sourceLocale("ko_KR")
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .publishedAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .current(current)
                .highlightsJson("[\"highlight\"]")
                .build();
        ReflectionTestUtils.setField(patchNote, "id", id);
        return patchNote;
    }

    private PatchChange patchChange(Long id, PatchNote patchNote) {
        return patchChange(id, patchNote, 1);
    }

    private PatchChange patchChange(Long id, PatchNote patchNote, int sortOrder) {
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .category(PatchChangeCategory.CHAMPION)
                .changeType(PatchChangeType.BUFF)
                .impact(PatchChangeImpact.HIGH)
                .targetKey("tft17_jinx")
                .targetName("Jinx")
                .summary("Jinx buff")
                .beforeValue("10")
                .afterValue("20")
                .imageUrl("https://example.com/jinx.png")
                .tagsJson("[\"champion\"]")
                .sortOrder(sortOrder)
                .build();
        ReflectionTestUtils.setField(patchChange, "id", id);
        return patchChange;
    }

    private PatchChange importedPatchChange(Long id, PatchNote patchNote) {
        return importedPatchChange(
                id,
                patchNote,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                1
        );
    }

    private PatchChange importedPatchChange(Long id, PatchNote patchNote, String sourceKey, int sourceOrder) {
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .sourceKey(sourceKey)
                .sourceHeadingPath("Champion > Jinx")
                .sourceOrder(sourceOrder)
                .importedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .category(PatchChangeCategory.CHAMPION)
                .changeType(PatchChangeType.BUFF)
                .impact(PatchChangeImpact.HIGH)
                .targetKey("tft17_jinx")
                .targetName("Jinx")
                .summary("Jinx buff")
                .beforeValue("10")
                .afterValue("20")
                .imageUrl("https://example.com/jinx.png")
                .tagsJson("[\"champion\"]")
                .sortOrder(sourceOrder)
                .build();
        ReflectionTestUtils.setField(patchChange, "id", id);
        return patchChange;
    }

    private AdminPatchNoteImportRequest patchNoteImportRequest(String sourceUrl, boolean current) {
        AdminPatchNoteImportRequest request = new AdminPatchNoteImportRequest();
        ReflectionTestUtils.setField(request, "sourceUrl", sourceUrl);
        ReflectionTestUtils.setField(request, "current", current);
        return request;
    }

    private PatchNoteCrawlDocument patchNoteCrawlDocument(
            PatchNoteCrawlFetchedPage detailPage,
            List<PatchChangeCrawlRow> rows,
            List<String> parserWarnings
    ) {
        return new PatchNoteCrawlDocument(
                detailPage.sourceUrl(),
                "ko-kr",
                "riot-content-17-3",
                "17.3 Patch Notes",
                "17.3",
                "Official summary",
                LocalDateTime.of(2026, 6, 15, 9, 0),
                "https://example.com/patch.png",
                List.of("Riot"),
                List.of("Champions"),
                rows,
                parserWarnings
        );
    }

    private PatchChangeCrawlRow patchChangeCrawlRow(String sourceKey, int sourceOrder) {
        return new PatchChangeCrawlRow(
                "candidate-" + sourceKey,
                sourceKey,
                "Champions > Jinx",
                sourceOrder,
                "Champions",
                "Jinx",
                "Jinx attack damage changed " + sourceOrder,
                "<li>Jinx attack damage changed</li>",
                "50",
                "55",
                List.of()
        );
    }

    private GuideChampion guideChampion(String championKey, String name, String patchVersion) {
        return GuideChampion.builder()
                .championKey(championKey)
                .name(name)
                .cost(4)
                .role("carry")
                .position("backline")
                .imageUrl("https://example.com/champion.png")
                .statsJson("{}")
                .traitsJson("[]")
                .bestItemsJson("[]")
                .patchVersion(patchVersion)
                .build();
    }

    private GuideTrait guideTrait(String traitKey, String name, String patchVersion) {
        return GuideTrait.builder()
                .traitKey(traitKey)
                .name(name)
                .type("origin")
                .iconUrl("https://example.com/trait.png")
                .tone("cyan")
                .summary("trait summary")
                .levelsJson("[]")
                .tierEffectsJson("[]")
                .championsJson("[]")
                .specialUnitsJson("[]")
                .tipsJson("[]")
                .patchVersion(patchVersion)
                .build();
    }

    private PatchNoteCrawlFetchedPage fetchedPage(String sourceUrl) {
        return new PatchNoteCrawlFetchedPage(
                sourceUrl,
                "<html></html>",
                LocalDateTime.of(2026, 6, 16, 9, 0),
                200
        );
    }

    private AdminPatchNoteRequest patchNoteRequest(String version, boolean current, List<String> highlights) {
        AdminPatchNoteRequest request = new AdminPatchNoteRequest();
        ReflectionTestUtils.setField(request, "version", version);
        ReflectionTestUtils.setField(request, "title", version + " patch");
        ReflectionTestUtils.setField(request, "summary", "summary");
        ReflectionTestUtils.setField(request, "description", "description");
        ReflectionTestUtils.setField(request, "focus", "balance");
        ReflectionTestUtils.setField(request, "imageUrl", "https://example.com/patch.png");
        ReflectionTestUtils.setField(request, "publishedAt", LocalDateTime.of(2026, 6, 1, 9, 0));
        ReflectionTestUtils.setField(request, "current", current);
        ReflectionTestUtils.setField(request, "highlights", highlights);
        return request;
    }

    private AdminPatchChangeRequest patchChangeRequest(Long patchNoteId, String category) {
        AdminPatchChangeRequest request = new AdminPatchChangeRequest();
        ReflectionTestUtils.setField(request, "patchNoteId", patchNoteId);
        ReflectionTestUtils.setField(request, "category", category);
        ReflectionTestUtils.setField(request, "type", "BUFF");
        ReflectionTestUtils.setField(request, "impact", "HIGH");
        ReflectionTestUtils.setField(request, "targetKey", "tft17_jinx");
        ReflectionTestUtils.setField(request, "targetName", "Jinx");
        ReflectionTestUtils.setField(request, "summary", "Jinx buff");
        ReflectionTestUtils.setField(request, "beforeValue", "10");
        ReflectionTestUtils.setField(request, "afterValue", "20");
        ReflectionTestUtils.setField(request, "imageUrl", "https://example.com/jinx.png");
        ReflectionTestUtils.setField(request, "tags", List.of("champion"));
        ReflectionTestUtils.setField(request, "sortOrder", 1);
        return request;
    }
}
