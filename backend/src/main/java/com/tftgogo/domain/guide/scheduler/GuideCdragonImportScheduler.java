package com.tftgogo.domain.guide.scheduler;

import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
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
public class GuideCdragonImportScheduler {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportScheduler.class);

    private final GuideCdragonImportService guideCdragonImportService;
    private final GuideCdragonImportProperties guideCdragonImportProperties;
    private final GuideCdragonImportSchedulerLock schedulerLock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void importOnStartupIfEnabled() {
        if (!guideCdragonImportProperties.isEnabled()) {
            logger.info("Guide CDragon scheduler disabled (app.guide.cdragon.enabled=false)");
            return;
        }
        if (!guideCdragonImportProperties.isStartupImport()) {
            logger.info("Guide CDragon startup import disabled (app.guide.cdragon.startup-import=false)");
            return;
        }
        runIfIdle("startup");
    }

    @Scheduled(
            cron = "${app.guide.cdragon.sync-cron:0 10 * * * *}",
            zone = "${app.guide.cdragon.zone:Asia/Seoul}"
    )
    public void syncLatestGuides() {
        runIfEnabledAndIdle("sync");
    }

    @Scheduled(
            cron = "${app.guide.cdragon.refresh-cron:0 40 6 * * *}",
            zone = "${app.guide.cdragon.zone:Asia/Seoul}"
    )
    public void refreshLatestGuides() {
        runIfEnabledAndIdle("daily-refresh");
    }

    private void runIfEnabledAndIdle(String trigger) {
        if (!guideCdragonImportProperties.isEnabled()) {
            return;
        }
        runIfIdle(trigger);
    }

    private void runIfIdle(String trigger) {
        if (!hasExplicitSourceConfiguration()) {
            logger.warn(
                    "Guide CDragon import skipped because set number and mutator are not explicitly configured. "
                            + "trigger={}, setNumber={}",
                    trigger,
                    guideCdragonImportProperties.getSetNumber()
            );
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Guide CDragon import skipped because another import is running. trigger={}", trigger);
            return;
        }

        try {
            schedulerLock.runWithLock(trigger, () -> importGuides(trigger));
        } finally {
            running.set(false);
        }
    }

    private void importGuides(String trigger) {
        GuideCdragonImportRequest request = GuideCdragonImportRequest.of(
                guideCdragonImportProperties.getPatchVersion(),
                guideCdragonImportProperties.getSetNumber(),
                guideCdragonImportProperties.getMutator(),
                guideCdragonImportProperties.isIncludeChampions(),
                guideCdragonImportProperties.isIncludeTraits(),
                guideCdragonImportProperties.isIncludeItems(),
                guideCdragonImportProperties.isIncludeAugments()
        );

        try {
            GuideImportResponse response = guideCdragonImportService.importGuides(request);
            logger.info(
                    "Guide CDragon import completed. trigger={}, patchVersion={}, setNumber={}, mutator={}, "
                            + "created={}, updated={}, skipped={}, champions={}, traits={}, items={}, augments={}",
                    trigger,
                    response.getPatchVersion(),
                    response.getSetNumber(),
                    response.getMutator(),
                    response.getCreatedCount(),
                    response.getUpdatedCount(),
                    response.getSkippedCount(),
                    response.getChampionCount(),
                    response.getTraitCount(),
                    response.getItemCount(),
                    response.getAugmentCount()
            );
        } catch (Exception e) {
            logger.warn(
                    "Guide CDragon import failed. Server will continue. trigger={}, patchVersion={}",
                    trigger,
                    guideCdragonImportProperties.getPatchVersion(),
                    e
            );
        }
    }

    private boolean hasExplicitSourceConfiguration() {
        Integer setNumber = guideCdragonImportProperties.getSetNumber();
        String mutator = guideCdragonImportProperties.getMutator();
        return setNumber != null
                && setNumber > 0
                && mutator != null
                && !mutator.trim().isEmpty()
                && mutator.trim().length() <= 100;
    }
}
