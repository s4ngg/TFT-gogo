package com.tftgogo.global.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class GuideFlywayConfiguration {

    @Bean
    public FlywayConfigurationCustomizer guideSnapshotMinimumCountPlaceholders(
            GuideCdragonImportProperties properties
    ) {
        return configuration -> {
            Map<String, String> placeholders = new HashMap<>(configuration.getPlaceholders());
            placeholders.put(
                    "guideMinimumChampionCount",
                    String.valueOf(properties.getMinimumChampionCount())
            );
            placeholders.put(
                    "guideMinimumTraitCount",
                    String.valueOf(properties.getMinimumTraitCount())
            );
            placeholders.put(
                    "guideMinimumItemCount",
                    String.valueOf(properties.getMinimumItemCount())
            );
            placeholders.put(
                    "guideMinimumAugmentCount",
                    String.valueOf(properties.getMinimumAugmentCount())
            );
            configuration.placeholders(placeholders);
        };
    }
}
