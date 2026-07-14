package com.tftgogo.domain.content.scheduler;

import com.tftgogo.domain.guide.scheduler.GuideCdragonImportTask;
import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.scheduler.PatchNoteImportTask;
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
        AdminPatchNoteImportResponse patchResponse = backfillHistory
                ? patchNoteImportTask.importLatestPatchNoteThenUnknownPatchNotesFromList()
                : patchNoteImportTask.importLatestPatchNote();
        String committedPatchVersion = requirePatchVersion(patchResponse);
        logger.info(
                "Content refresh patch step committed. trigger={}, patchVersion={}",
                trigger,
                committedPatchVersion
        );

        if (!importGuide) {
            logger.info("Content refresh guide step disabled. trigger={}, patchVersion={}", trigger, committedPatchVersion);
            return;
        }

        guideCdragonImportTask.importGuides(trigger, committedPatchVersion);
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
