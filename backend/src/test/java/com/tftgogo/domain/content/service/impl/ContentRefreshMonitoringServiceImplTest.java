package com.tftgogo.domain.content.service.impl;

import com.tftgogo.domain.content.config.ContentRefreshMonitoringProperties;
import com.tftgogo.domain.content.dto.response.ContentRefreshHealthResponse;
import com.tftgogo.domain.content.entity.ContentRefreshFailureType;
import com.tftgogo.domain.content.entity.ContentRefreshJobStatus;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;
import com.tftgogo.domain.content.entity.ContentRefreshRunStatus;
import com.tftgogo.domain.content.repository.ContentRefreshJobStatusRepository;
import com.tftgogo.domain.guide.entity.GuideSnapshot;
import com.tftgogo.domain.guide.entity.GuideSnapshotStatus;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository.GuideDataCounts;
import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentRefreshMonitoringServiceImplTest {

    @Mock
    private ContentRefreshJobStatusRepository statusRepository;

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private PatchChangeRepository patchChangeRepository;

    @Mock
    private GuideSnapshotRepository guideSnapshotRepository;

    private PatchNoteImportSchedulerProperties patchNoteProperties;
    private GuideCdragonImportProperties guideProperties;
    private ContentRefreshMonitoringProperties monitoringProperties;
    private ContentRefreshMonitoringServiceImpl monitoringService;

    @BeforeEach
    void setUp() {
        patchNoteProperties = new PatchNoteImportSchedulerProperties();
        patchNoteProperties.setEnabled(true);
        guideProperties = new GuideCdragonImportProperties();
        guideProperties.setEnabled(true);
        guideProperties.setSetNumber(17);
        guideProperties.setMutator("TFTSet17");
        monitoringProperties = new ContentRefreshMonitoringProperties();
        monitoringService = new ContentRefreshMonitoringServiceImpl(
                statusRepository,
                patchNoteRepository,
                patchChangeRepository,
                guideSnapshotRepository,
                patchNoteProperties,
                guideProperties,
                monitoringProperties
        );
    }

    @Test
    void 최초_시도와_성공_상태를_저장한다() {
        // given
        when(statusRepository.findById(ContentRefreshJobType.PATCH_NOTE)).thenReturn(Optional.empty());
        ArgumentCaptor<ContentRefreshJobStatus> captor = ArgumentCaptor.forClass(ContentRefreshJobStatus.class);

        // when
        monitoringService.recordAttempt(ContentRefreshJobType.PATCH_NOTE);

        // then
        verify(statusRepository).save(captor.capture());
        ContentRefreshJobStatus status = captor.getValue();
        assertThat(status.getLastStatus()).isEqualTo(ContentRefreshRunStatus.RUNNING);
        assertThat(status.getLastAttemptAt()).isNotNull();

        // when
        when(statusRepository.findById(ContentRefreshJobType.PATCH_NOTE)).thenReturn(Optional.of(status));
        monitoringService.recordSuccess(ContentRefreshJobType.PATCH_NOTE, "17.5", 100L);

        // then
        assertThat(status.getLastStatus()).isEqualTo(ContentRefreshRunStatus.SUCCESS);
        assertThat(status.getLastSuccessVersion()).isEqualTo("17.5");
        assertThat(status.getLastSuccessCount()).isEqualTo(100L);
        assertThat(status.getConsecutiveFailureCount()).isZero();
    }

    @Test
    void 연속_실패를_누적하고_외부소스_실패로_분류한다() {
        // given
        ContentRefreshJobStatus status = ContentRefreshJobStatus.create(ContentRefreshJobType.PATCH_NOTE);
        status.recordSuccess("17.4", 90L, LocalDateTime.now().minusHours(1));
        when(statusRepository.findById(ContentRefreshJobType.PATCH_NOTE)).thenReturn(Optional.of(status));

        // when
        monitoringService.recordFailure(
                ContentRefreshJobType.PATCH_NOTE,
                new BusinessException(ErrorCode.EXTERNAL_API_ERROR)
        );
        monitoringService.recordFailure(
                ContentRefreshJobType.PATCH_NOTE,
                new BusinessException(ErrorCode.EXTERNAL_API_ERROR)
        );

        // then
        assertThat(status.getLastStatus()).isEqualTo(ContentRefreshRunStatus.FAILURE);
        assertThat(status.getConsecutiveFailureCount()).isEqualTo(2);
        assertThat(status.getLastFailureType()).isEqualTo(ContentRefreshFailureType.SOURCE_UNAVAILABLE);
        assertThat(status.getLastSuccessVersion()).isEqualTo("17.4");
        assertThat(status.getLastSuccessCount()).isEqualTo(90L);
    }

    @Test
    void history_부분_실패는_최근_성공정보를_보존하고_경고로_기록한다() {
        // given
        ContentRefreshJobStatus status = ContentRefreshJobStatus.create(ContentRefreshJobType.PATCH_NOTE);
        status.recordSuccess("17.5", 100L, LocalDateTime.now());
        LocalDateTime previousSuccessAt = status.getLastSuccessAt();
        when(statusRepository.findById(ContentRefreshJobType.PATCH_NOTE)).thenReturn(Optional.of(status));

        // when
        monitoringService.recordPartialSuccess(
                ContentRefreshJobType.PATCH_NOTE,
                "17.5",
                100L,
                ContentRefreshFailureType.HISTORY_BACKFILL
        );
        monitoringService.recordPartialSuccess(
                ContentRefreshJobType.PATCH_NOTE,
                "17.6",
                110L,
                ContentRefreshFailureType.HISTORY_BACKFILL
        );

        // then
        assertThat(status.getLastStatus()).isEqualTo(ContentRefreshRunStatus.FAILURE);
        assertThat(status.getConsecutiveFailureCount()).isEqualTo(2);
        assertThat(status.getLastFailureType()).isEqualTo(ContentRefreshFailureType.HISTORY_BACKFILL);
        assertThat(status.getLastSuccessVersion()).isEqualTo("17.6");
        assertThat(status.getLastSuccessCount()).isEqualTo(110L);
        assertThat(status.getLastSuccessAt()).isAfterOrEqualTo(previousSuccessAt);
        assertThat(status.getLastFailureAt()).isEqualTo(status.getLastSuccessAt());

        // when
        monitoringService.recordSuccess(ContentRefreshJobType.PATCH_NOTE, "17.6", 110L);

        // then
        assertThat(status.getLastStatus()).isEqualTo(ContentRefreshRunStatus.SUCCESS);
        assertThat(status.getConsecutiveFailureCount()).isZero();
        assertThat(status.getLastFailureType()).isEqualTo(ContentRefreshFailureType.HISTORY_BACKFILL);
    }

    @Test
    void history_부분_실패가_임계값에_도달하면_critical로_표시한다() {
        // given
        guideProperties.setEnabled(false);
        ContentRefreshJobStatus status = ContentRefreshJobStatus.create(ContentRefreshJobType.PATCH_NOTE);
        status.recordSuccess("17.4", 90L, LocalDateTime.now().minusHours(1));
        when(statusRepository.findById(ContentRefreshJobType.PATCH_NOTE)).thenReturn(Optional.of(status));

        monitoringService.recordPartialSuccess(
                ContentRefreshJobType.PATCH_NOTE,
                "17.5",
                100L,
                ContentRefreshFailureType.HISTORY_BACKFILL
        );
        monitoringService.recordPartialSuccess(
                ContentRefreshJobType.PATCH_NOTE,
                "17.6",
                110L,
                ContentRefreshFailureType.HISTORY_BACKFILL
        );
        monitoringService.recordPartialSuccess(
                ContentRefreshJobType.PATCH_NOTE,
                "17.7",
                120L,
                ContentRefreshFailureType.HISTORY_BACKFILL
        );

        PatchNote currentPatch = currentPatch("17.7");
        when(statusRepository.findAll()).thenReturn(List.of(status));
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(currentPatch));
        when(patchChangeRepository.countByPatchNote(currentPatch)).thenReturn(120L);
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        ContentRefreshHealthResponse response = monitoringService.getHealth();

        // then
        ContentRefreshHealthResponse.JobHealth patchHealth = response.getJobs().get(0);
        assertThat(response.getStatus()).isEqualTo("CRITICAL");
        assertThat(patchHealth.getStatus()).isEqualTo("CRITICAL");
        assertThat(patchHealth.getAlertReasons()).containsExactly("CONSECUTIVE_FAILURES");
        assertThat(patchHealth.getConsecutiveFailureCount()).isEqualTo(3);
        assertThat(patchHealth.getLastFailureType()).isEqualTo(ContentRefreshFailureType.HISTORY_BACKFILL);
        assertThat(patchHealth.getLastSuccessVersion()).isEqualTo("17.7");
        assertThat(patchHealth.getLastSuccessCount()).isEqualTo(120L);
    }

    @Test
    void 최신_데이터와_실행_상태가_정상이면_healthy를_반환한다() {
        // given
        ContentRefreshJobStatus patchStatus = successfulStatus(ContentRefreshJobType.PATCH_NOTE, "17.5", 100L);
        ContentRefreshJobStatus guideStatus = successfulStatus(ContentRefreshJobType.GAME_GUIDE, "17.5", 140L);
        PatchNote currentPatch = currentPatch("17.5");
        GuideSnapshot activeGuide = activeGuide("17.5", 40, 20, 30, 50);
        when(statusRepository.findAll()).thenReturn(List.of(patchStatus, guideStatus));
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(currentPatch));
        when(patchChangeRepository.countByPatchNote(currentPatch)).thenReturn(100L);
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(activeGuide));
        when(guideSnapshotRepository.countGuideDataByPatchVersion("17.5"))
                .thenReturn(guideDataCounts(40, 20, 30, 50));

        // when
        ContentRefreshHealthResponse response = monitoringService.getHealth();

        // then
        assertThat(response.getStatus()).isEqualTo("HEALTHY");
        assertThat(response.getJobs())
                .extracting(ContentRefreshHealthResponse.JobHealth::getStatus)
                .containsExactly("HEALTHY", "HEALTHY");
        assertThat(response.getJobs())
                .allSatisfy(job -> assertThat(job.getAlertReasons()).isEmpty());
    }

    @Test
    void 오래된_성공과_빈_패치_불완전_가이드를_critical로_표시한다() {
        // given
        ContentRefreshJobStatus patchStatus = staleSuccessfulStatus(ContentRefreshJobType.PATCH_NOTE, "17.4", 100L);
        ContentRefreshJobStatus guideStatus = successfulStatus(ContentRefreshJobType.GAME_GUIDE, "17.4", 4L);
        PatchNote currentPatch = currentPatch("17.5");
        GuideSnapshot activeGuide = activeGuide("17.4", 40, 20, 30, 50);
        when(statusRepository.findAll()).thenReturn(List.of(patchStatus, guideStatus));
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(currentPatch));
        when(patchChangeRepository.countByPatchNote(currentPatch)).thenReturn(0L);
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(activeGuide));
        when(guideSnapshotRepository.countGuideDataByPatchVersion("17.4"))
                .thenReturn(guideDataCounts(1, 1, 1, 1));

        // when
        ContentRefreshHealthResponse response = monitoringService.getHealth();

        // then
        assertThat(response.getStatus()).isEqualTo("CRITICAL");
        assertThat(response.getJobs().get(0).getAlertReasons())
                .contains("STALE", "CURRENT_PATCH_CHANGES_EMPTY");
        assertThat(response.getJobs().get(1).getAlertReasons())
                .contains(
                        "ACTIVE_GUIDE_SNAPSHOT_INCOMPLETE",
                        "ACTIVE_GUIDE_COUNT_MISMATCH",
                        "PATCH_GUIDE_VERSION_MISMATCH"
                );
    }

    @Test
    void scheduler가_꺼져있으면_데이터가_없어도_disabled로_표시한다() {
        // given
        patchNoteProperties.setEnabled(false);
        when(statusRepository.findAll()).thenReturn(List.of());
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.empty());
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        ContentRefreshHealthResponse response = monitoringService.getHealth();

        // then
        assertThat(response.getStatus()).isEqualTo("DISABLED");
        assertThat(response.getJobs())
                .extracting(ContentRefreshHealthResponse.JobHealth::getStatus)
                .containsExactly("DISABLED", "DISABLED");
        assertThat(response.getJobs())
                .allSatisfy(job -> assertThat(job.getAlertReasons()).isEmpty());
    }

    @Test
    void 연속_실패는_임계값_전에는_warning이고_도달하면_critical이다() {
        // given
        guideProperties.setEnabled(false);
        ContentRefreshJobStatus patchStatus = successfulStatus(ContentRefreshJobType.PATCH_NOTE, "17.5", 100L);
        patchStatus.recordFailure(ContentRefreshFailureType.SOURCE_UNAVAILABLE, LocalDateTime.now());
        patchStatus.recordFailure(ContentRefreshFailureType.SOURCE_UNAVAILABLE, LocalDateTime.now());
        PatchNote currentPatch = currentPatch("17.5");
        when(statusRepository.findAll()).thenReturn(List.of(patchStatus));
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(currentPatch));
        when(patchChangeRepository.countByPatchNote(currentPatch)).thenReturn(100L);
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        ContentRefreshHealthResponse warning = monitoringService.getHealth();

        // then
        assertThat(warning.getStatus()).isEqualTo("WARNING");
        assertThat(warning.getJobs().get(0).getAlertReasons()).containsExactly("RECENT_FAILURE");

        // when
        patchStatus.recordFailure(ContentRefreshFailureType.SOURCE_UNAVAILABLE, LocalDateTime.now());
        ContentRefreshHealthResponse critical = monitoringService.getHealth();

        // then
        assertThat(critical.getStatus()).isEqualTo("CRITICAL");
        assertThat(critical.getJobs().get(0).getAlertReasons()).containsExactly("CONSECUTIVE_FAILURES");
    }

    @Test
    void 제한시간_안의_실행은_running이고_초과하면_stuck으로_판정한다() {
        // given
        guideProperties.setEnabled(false);
        ContentRefreshJobStatus patchStatus = ContentRefreshJobStatus.create(ContentRefreshJobType.PATCH_NOTE);
        patchStatus.recordAttempt(LocalDateTime.now());
        PatchNote currentPatch = currentPatch("17.5");
        when(statusRepository.findAll()).thenReturn(List.of(patchStatus));
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(currentPatch));
        when(patchChangeRepository.countByPatchNote(currentPatch)).thenReturn(100L);
        when(guideSnapshotRepository.findFirstByStatus(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        ContentRefreshHealthResponse running = monitoringService.getHealth();

        // then
        assertThat(running.getStatus()).isEqualTo("RUNNING");
        assertThat(running.getJobs().get(0).getAlertReasons()).isEmpty();

        // when
        patchStatus.recordAttempt(
                LocalDateTime.now().minusMinutes(monitoringProperties.getRunningTimeoutMinutes() + 1)
        );
        ContentRefreshHealthResponse stuck = monitoringService.getHealth();

        // then
        assertThat(stuck.getStatus()).isEqualTo("CRITICAL");
        assertThat(stuck.getJobs().get(0).getAlertReasons())
                .contains("NEVER_SUCCEEDED", "STUCK_RUNNING");
    }

    private ContentRefreshJobStatus successfulStatus(
            ContentRefreshJobType jobType,
            String version,
            long count
    ) {
        ContentRefreshJobStatus status = ContentRefreshJobStatus.create(jobType);
        status.recordAttempt(LocalDateTime.now());
        status.recordSuccess(version, count, LocalDateTime.now());
        return status;
    }

    private ContentRefreshJobStatus staleSuccessfulStatus(
            ContentRefreshJobType jobType,
            String version,
            long count
    ) {
        ContentRefreshJobStatus status = ContentRefreshJobStatus.create(jobType);
        LocalDateTime staleTime = LocalDateTime.now()
                .minusHours(monitoringProperties.getStaleAfterHours() + 1);
        status.recordAttempt(staleTime);
        status.recordSuccess(version, count, staleTime);
        return status;
    }

    private PatchNote currentPatch(String version) {
        return PatchNote.builder()
                .version(version)
                .title("패치 " + version)
                .description("content")
                .publishedAt(LocalDateTime.now())
                .current(true)
                .build();
    }

    private GuideSnapshot activeGuide(
            String version,
            int championCount,
            int traitCount,
            int itemCount,
            int augmentCount
    ) {
        return GuideSnapshot.builder()
                .patchVersion(version)
                .sourceSetNumber(17)
                .sourceMutator("TFTSet17")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(championCount)
                .traitCount(traitCount)
                .itemCount(itemCount)
                .augmentCount(augmentCount)
                .validatedAt(LocalDateTime.now())
                .activatedAt(LocalDateTime.now())
                .build();
    }

    private GuideDataCounts guideDataCounts(
            long championCount,
            long traitCount,
            long itemCount,
            long augmentCount
    ) {
        return new GuideDataCounts() {
            @Override
            public long getChampionCount() {
                return championCount;
            }

            @Override
            public long getTraitCount() {
                return traitCount;
            }

            @Override
            public long getItemCount() {
                return itemCount;
            }

            @Override
            public long getAugmentCount() {
                return augmentCount;
            }
        };
    }
}
