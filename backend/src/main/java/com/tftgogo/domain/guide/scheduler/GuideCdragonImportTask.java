package com.tftgogo.domain.guide.scheduler;

import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuideCdragonImportTask {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportTask.class);

    private final GuideCdragonImportService guideCdragonImportService;
    private final GuideCdragonImportProperties properties;

    public GuideImportResponse importGuides(String trigger, String committedPatchVersion) {
        GuideCdragonImportRequest request = GuideCdragonImportRequest.of(
                committedPatchVersion,
                properties.getSetNumber(),
                properties.getMutator(),
                properties.isIncludeChampions(),
                properties.isIncludeTraits(),
                properties.isIncludeItems(),
                properties.isIncludeAugments()
        );

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
        return response;
    }

    public boolean hasExplicitSourceConfiguration() {
        Integer setNumber = properties.getSetNumber();
        String mutator = properties.getMutator();
        return setNumber != null
                && setNumber > 0
                && mutator != null
                && !mutator.trim().isEmpty()
                && mutator.trim().length() <= 100;
    }
}
