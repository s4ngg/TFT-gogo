package com.tftgogo.global.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class GuideFlywayConfigurationTest {

    private final GuideFlywayConfiguration guideFlywayConfiguration = new GuideFlywayConfiguration();

    @Test
    void 런타임_최소_건수를_Flyway_마이그레이션에도_동일하게_전달한다() {
        // given
        GuideCdragonImportProperties properties = new GuideCdragonImportProperties();
        properties.setMinimumChampionCount(40);
        properties.setMinimumTraitCount(20);
        properties.setMinimumItemCount(30);
        properties.setMinimumAugmentCount(50);
        FluentConfiguration configuration = Flyway.configure();
        FlywayConfigurationCustomizer customizer = guideFlywayConfiguration
                .guideSnapshotMinimumCountPlaceholders(properties);

        // when
        customizer.customize(configuration);

        // then
        assertThat(configuration.getPlaceholders())
                .containsEntry("guideMinimumChampionCount", "40")
                .containsEntry("guideMinimumTraitCount", "20")
                .containsEntry("guideMinimumItemCount", "30")
                .containsEntry("guideMinimumAugmentCount", "50");
    }

    @Test
    void Flyway_최소_건수는_0이_설정되어도_1보다_작아지지_않는다() {
        // given
        GuideCdragonImportProperties properties = new GuideCdragonImportProperties();
        properties.setMinimumChampionCount(0);
        FluentConfiguration configuration = Flyway.configure();

        // when
        guideFlywayConfiguration.guideSnapshotMinimumCountPlaceholders(properties)
                .customize(configuration);

        // then
        assertThat(configuration.getPlaceholders())
                .containsEntry("guideMinimumChampionCount", "1");
    }
}
