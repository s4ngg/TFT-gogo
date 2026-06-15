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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideCdragonImportSchedulerTest {

    @Mock
    private GuideCdragonImportService guideCdragonImportService;

    private GuideCdragonImportProperties guideCdragonImportProperties;
    private GuideCdragonImportScheduler scheduler;

    @BeforeEach
    void setUp() {
        guideCdragonImportProperties = new GuideCdragonImportProperties();
        scheduler = new GuideCdragonImportScheduler(guideCdragonImportService, guideCdragonImportProperties);
    }

    @Test
    void startup_import가_false면_CDragon_import를_실행하지_않는다() {
        // when
        scheduler.importOnStartupIfEnabled();

        // then
        verifyNoInteractions(guideCdragonImportService);
    }

    @Test
    void startup_import가_true면_설정값으로_CDragon_import를_실행한다() {
        // given
        guideCdragonImportProperties.setStartupImport(true);
        guideCdragonImportProperties.setPatchVersion("17.4");
        guideCdragonImportProperties.setSetNumber(17);
        guideCdragonImportProperties.setMutator("TFTSet17");
        guideCdragonImportProperties.setIncludeChampions(true);
        guideCdragonImportProperties.setIncludeTraits(false);
        guideCdragonImportProperties.setIncludeItems(true);
        guideCdragonImportProperties.setIncludeAugments(true);
        when(guideCdragonImportService.importGuides(any(GuideCdragonImportRequest.class)))
                .thenReturn(GuideImportResponse.builder()
                        .createdCount(1)
                        .updatedCount(2)
                        .skippedCount(3)
                        .championCount(4)
                        .traitCount(5)
                        .itemCount(6)
                        .augmentCount(7)
                        .build());

        // when
        scheduler.importOnStartupIfEnabled();

        // then
        ArgumentCaptor<GuideCdragonImportRequest> requestCaptor =
                ArgumentCaptor.forClass(GuideCdragonImportRequest.class);
        verify(guideCdragonImportService).importGuides(requestCaptor.capture());
        GuideCdragonImportRequest request = requestCaptor.getValue();
        assertThat(request.getPatchVersion()).isEqualTo("17.4");
        assertThat(request.resolveSetNumber()).isEqualTo(17);
        assertThat(request.resolveMutator()).isEqualTo("TFTSet17");
        assertThat(request.shouldIncludeChampions()).isTrue();
        assertThat(request.shouldIncludeTraits()).isFalse();
        assertThat(request.shouldIncludeItems()).isTrue();
        assertThat(request.shouldIncludeAugments()).isTrue();
    }

    @Test
    void startup_import_실패는_서버_기동을_막지_않는다() {
        // given
        guideCdragonImportProperties.setStartupImport(true);
        when(guideCdragonImportService.importGuides(any(GuideCdragonImportRequest.class)))
                .thenThrow(new RuntimeException("cdragon unavailable"));

        // when, then
        assertThatCode(() -> scheduler.importOnStartupIfEnabled())
                .doesNotThrowAnyException();
    }
}
