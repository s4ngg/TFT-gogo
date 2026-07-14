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
                    String.valueOf(minimumRequired(properties.getMinimumChampionCount()))
            );
            placeholders.put(
                    "guideMinimumTraitCount",
                    String.valueOf(minimumRequired(properties.getMinimumTraitCount()))
            );
            placeholders.put(
                    "guideMinimumItemCount",
                    String.valueOf(minimumRequired(properties.getMinimumItemCount()))
            );
            placeholders.put(
                    "guideMinimumAugmentCount",
                    String.valueOf(minimumRequired(properties.getMinimumAugmentCount()))
            );
            configuration.placeholders(placeholders);
        };
    }

    private int minimumRequired(int configuredMinimum) {
        return Math.max(1, configuredMinimum);
    }
}
