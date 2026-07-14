package com.tftgogo.domain.content.scheduler;

import com.tftgogo.domain.content.entity.ContentRefreshFailureType;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;
import com.tftgogo.domain.content.service.ContentRefreshMonitoringService;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.scheduler.GuideCdragonImportTask;
import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask.PatchNoteRefreshResult;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ContentRefreshScheduler {

    private static final Logger logger = LogManager.getLogger(ContentRefreshScheduler.class);

    private final PatchNoteImportTask patchNoteImportTask;
    private final GuideCdragonImportTask guideCdragonImportTask;
    private final PatchNoteImportSchedulerProperties patchNoteProperties;
    private final GuideCdragonImportProperties guideProperties;
    private final ContentRefreshSchedulerLock schedulerLock;
    private final ContentRefreshMonitoringService monitoringService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void importOnStartupIfEnabled() {
        if (!patchNoteProperties.isEnabled()) {
            logger.info("Content refresh scheduler disabled (app.patch-note.scheduler.enabled=false)");
            return;
        }
        if (!patchNoteProperties.isStartupImport()) {
            logger.info("Content refresh startup import disabled (app.patch-note.scheduler.startup-import=false)");
            return;
        }

        boolean importGuide = guideProperties.isEnabled() && guideProperties.isStartupImport();
        runIfIdle("startup", true, importGuide);
    }

    @Scheduled(
            cron = "${app.patch-note.scheduler.list-cron:0 0 * * * *}",
            zone = "${app.patch-note.scheduler.zone:Asia/Seoul}"
    )
    public void syncContent() {
        runIfEnabledAndIdle("sync", true, guideProperties.isEnabled());
    }

    @Scheduled(
            cron = "${app.patch-note.scheduler.refresh-cron:0 30 6 * * *}",
            zone = "${app.patch-note.scheduler.zone:Asia/Seoul}"
    )
    public void refreshContent() {
        runIfEnabledAndIdle("daily-refresh", false, guideProperties.isEnabled());
    }

    private void runIfEnabledAndIdle(String trigger, boolean backfillHistory, boolean importGuide) {
        if (!patchNoteProperties.isEnabled()) {
            return;
        }
        runIfIdle(trigger, backfillHistory, importGuide);
    }

    private void runIfIdle(String trigger, boolean backfillHistory, boolean importGuide) {
        if (importGuide && !guideCdragonImportTask.hasExplicitSourceConfiguration()) {
            logger.error(
                    "Content refresh skipped because guide set number and mutator are not explicitly configured. "
                            + "trigger={}, setNumber={}",
                    trigger,
                    guideProperties.getSetNumber()
            );
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Content refresh skipped because another refresh is running. trigger={}", trigger);
            return;
        }

        try {
            schedulerLock.runWithLock(
                    trigger,
                    () -> refreshContentUnderLock(trigger, backfillHistory, importGuide)
            );
        } catch (Exception e) {
            logger.error("Content refresh failed. trigger={}", trigger, e);
        } finally {
            running.set(false);
        }
    }

    private void refreshContentUnderLock(String trigger, boolean backfillHistory, boolean importGuide) {
        AdminPatchNoteImportResponse patchResponse = importPatchNotes(backfillHistory);
        String committedPatchVersion = patchResponse.getVersion().trim();
        logger.info(
                "Content refresh patch step committed. trigger={}, patchVersion={}",
                trigger,
                committedPatchVersion
        );

        if (!importGuide) {
            logger.info("Content refresh guide step disabled. trigger={}, patchVersion={}", trigger, committedPatchVersion);
            return;
        }

        importGuides(trigger, committedPatchVersion);
    }

    private AdminPatchNoteImportResponse importPatchNotes(boolean backfillHistory) {
        recordAttemptSafely(ContentRefreshJobType.PATCH_NOTE);
        try {
            PatchNoteRefreshResult refreshResult = backfillHistory
                    ? patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList()
                    : null;
            AdminPatchNoteImportResponse response = backfillHistory
                    ? refreshResult.latestPatchNote()
                    : patchNoteImportTask.importLatestPatchNote();
            String version = requirePatchVersion(response);
            long processedCount = (long) response.getCreatedChanges()
                    + response.getUpdatedChanges()
                    + response.getSkippedChanges();
            if (refreshResult != null && refreshResult.hasHistoryFailures()) {
                recordPartialSuccessSafely(
                        ContentRefreshJobType.PATCH_NOTE,
                        version,
                        processedCount,
                        ContentRefreshFailureType.HISTORY_BACKFILL
                );
            } else {
                recordSuccessSafely(ContentRefreshJobType.PATCH_NOTE, version, processedCount);
            }
            return response;
        } catch (RuntimeException e) {
            recordFailureSafely(ContentRefreshJobType.PATCH_NOTE, e);
            throw e;
        }
    }

    private void importGuides(String trigger, String committedPatchVersion) {
        recordAttemptSafely(ContentRefreshJobType.GAME_GUIDE);
        try {
            GuideImportResponse response = guideCdragonImportTask.importGuides(trigger, committedPatchVersion);
            long processedCount = (long) response.getChampionCount()
                    + response.getTraitCount()
                    + response.getItemCount()
                    + response.getAugmentCount();
            recordSuccessSafely(
                    ContentRefreshJobType.GAME_GUIDE,
                    response.getPatchVersion(),
                    processedCount
            );
        } catch (RuntimeException e) {
            recordFailureSafely(ContentRefreshJobType.GAME_GUIDE, e);
            throw e;
        }
    }

    private void recordAttemptSafely(ContentRefreshJobType jobType) {
        try {
            monitoringService.recordAttempt(jobType);
        } catch (RuntimeException e) {
            logger.error("Content refresh attempt status recording failed. jobType={}", jobType, e);
        }
    }

    private void recordSuccessSafely(ContentRefreshJobType jobType, String version, long processedCount) {
        try {
            monitoringService.recordSuccess(jobType, version, processedCount);
        } catch (RuntimeException e) {
            logger.error(
                    "Content refresh success status recording failed. jobType={}, version={}, processedCount={}",
                    jobType,
                    version,
                    processedCount,
                    e
            );
        }
    }

    private void recordFailureSafely(ContentRefreshJobType jobType, RuntimeException failure) {
        try {
            monitoringService.recordFailure(jobType, failure);
        } catch (RuntimeException recordingFailure) {
            logger.error("Content refresh failure status recording failed. jobType={}", jobType, recordingFailure);
        }
    }

    private void recordPartialSuccessSafely(
            ContentRefreshJobType jobType,
            String version,
            long processedCount,
            ContentRefreshFailureType failureType
    ) {
        try {
            monitoringService.recordPartialSuccess(jobType, version, processedCount, failureType);
        } catch (RuntimeException recordingFailure) {
            logger.error("Content refresh partial success status recording failed. jobType={}", jobType, recordingFailure);
        }
    }

    private String requirePatchVersion(AdminPatchNoteImportResponse patchResponse) {
        if (patchResponse == null
                || patchResponse.getVersion() == null
                || patchResponse.getVersion().trim().isEmpty()) {
            throw new IllegalStateException("Committed patch note version is missing");
        }
        return patchResponse.getVersion().trim();
    }
}
