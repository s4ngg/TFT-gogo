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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuideCdragonImportScheduler {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportScheduler.class);

    private final GuideCdragonImportService guideCdragonImportService;
    private final GuideCdragonImportProperties guideCdragonImportProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void importOnStartupIfEnabled() {
        if (!guideCdragonImportProperties.isStartupImport()) {
            logger.info("Guide CDragon startup import disabled (app.guide.cdragon.startup-import=false)");
            return;
        }

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
                    "Guide CDragon startup import completed. patchVersion={}, created={}, updated={}, skipped={}, champions={}, traits={}, items={}, augments={}",
                    request.getPatchVersion(),
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
                    "Guide CDragon startup import failed. Server will continue. patchVersion={}",
                    guideCdragonImportProperties.getPatchVersion(),
                    e
            );
        }
    }
}
