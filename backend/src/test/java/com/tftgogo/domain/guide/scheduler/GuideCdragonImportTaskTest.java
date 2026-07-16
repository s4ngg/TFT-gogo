package com.tftgogo.domain.guide.scheduler;

import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
import com.tftgogo.global.config.GuideCdragonImportProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideCdragonImportTaskTest {

    @Mock
    private GuideCdragonImportService guideCdragonImportService;

    private GuideCdragonImportProperties properties;
    private GuideCdragonImportTask importTask;

    @BeforeEach
    void setUp() {
        properties = new GuideCdragonImportProperties();
        properties.setPatchVersion("latest");
        properties.setSetNumber(17);
        properties.setMutator("TFTSet17");
        importTask = new GuideCdragonImportTask(guideCdragonImportService, properties);
    }

    @Test
    void patch_task가_commit한_exact_version으로_가이드를_import한다() {
        // given
        GuideImportResponse response = importResponse("17.5");
        when(guideCdragonImportService.importGuides(any(GuideCdragonImportRequest.class)))
                .thenReturn(response);

        // when
        GuideImportResponse result = importTask.importGuides("sync", "17.5");

        // then
        ArgumentCaptor<GuideCdragonImportRequest> captor =
                ArgumentCaptor.forClass(GuideCdragonImportRequest.class);
        verify(guideCdragonImportService).importGuides(captor.capture());
        GuideCdragonImportRequest request = captor.getValue();
        assertThat(result).isSameAs(response);
        assertThat(request.getPatchVersion()).isEqualTo("17.5");
        assertThat(request.getPatchVersion()).isNotEqualTo(properties.getPatchVersion());
        assertThat(request.getSetNumber()).isEqualTo(17);
        assertThat(request.getMutator()).isEqualTo("TFTSet17");
        assertThat(request.shouldIncludeChampions()).isTrue();
        assertThat(request.shouldIncludeTraits()).isTrue();
        assertThat(request.shouldIncludeItems()).isTrue();
        assertThat(request.shouldIncludeAugments()).isTrue();
    }

    @Test
    void guide_import_실패를_호출자에게_전파한다() {
        // given
        when(guideCdragonImportService.importGuides(any(GuideCdragonImportRequest.class)))
                .thenThrow(new RuntimeException("cdragon unavailable"));

        // when, then
        assertThatThrownBy(() -> importTask.importGuides("sync", "17.5"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cdragon unavailable");
    }

    @Test
    void set과_mutator가_명시되었을_때만_source_configuration이_유효하다() {
        // when, then
        assertThat(importTask.hasExplicitSourceConfiguration()).isTrue();

        properties.setSetNumber(null);
        assertThat(importTask.hasExplicitSourceConfiguration()).isFalse();

        properties.setSetNumber(17);
        properties.setMutator("   ");
        assertThat(importTask.hasExplicitSourceConfiguration()).isFalse();

        properties.setMutator("a".repeat(101));
        assertThat(importTask.hasExplicitSourceConfiguration()).isFalse();
    }

    private GuideImportResponse importResponse(String patchVersion) {
        return GuideImportResponse.builder()
                .patchVersion(patchVersion)
                .setNumber(17)
                .mutator("TFTSet17")
                .createdCount(1)
                .updatedCount(2)
                .skippedCount(3)
                .championCount(4)
                .traitCount(5)
                .itemCount(6)
                .augmentCount(7)
                .build();
    }
}
