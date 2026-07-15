package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideSnapshot;
import com.tftgogo.domain.guide.entity.GuideSnapshotStatus;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideSnapshotRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    private static final LocalDateTime VALIDATED_AT = LocalDateTime.of(2026, 7, 14, 12, 0);

    @Mock
    private GuideChampionRepository guideChampionRepository;

    @Mock
    private GuideTraitRepository guideTraitRepository;

    @Mock
    private GuideItemRepository guideItemRepository;

    @Mock
    private GuideAugmentRepository guideAugmentRepository;

    @Mock
    private GuideSnapshotRepository guideSnapshotRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GuideServiceImpl guideService;

    @Test
    void champion_tab_uses_split_table_page_and_applies_cost_filter() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));
        when(guideChampionRepository.searchPage(eq("17.0"), isNull(), eq(4), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(champion("TFT17_Kaisa", "Kaisa", 4, "[\"Star Guardian\"]")),
                        PageRequest.of(0, 10),
                        1
                ));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                null,
                null,
                4
        );

        // then
        assertThat(response.getItems())
                .extracting(GuideEntryResponse::getTargetKey)
                .containsExactly("TFT17_Kaisa");
        assertThat(response.getTotalItems()).isEqualTo(1);
        verify(guideChampionRepository)
                .searchPage(eq("17.0"), isNull(), eq(4), any(Pageable.class));
    }

    @Test
    void 검증_완료된_과거_패치를_명시하면_INACTIVE_스냅샷을_조회한다() {
        // given
        when(guideSnapshotRepository.findByPatchVersion("17.0"))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.INACTIVE, VALIDATED_AT)));
        when(guideChampionRepository.searchPage(eq("17.0"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(champion("TFT17_Kaisa", "Kaisa", 4, "[\"Star Guardian\"]")),
                        PageRequest.of(0, 10),
                        1
                ));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "champions",
                "17.0",
                null,
                1,
                10,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        verify(guideSnapshotRepository, never()).findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE);
    }

    @Test
    void tab_items_return_page_metadata_from_repository_page() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));
        when(guideItemRepository.searchPage(eq("17.0"), eq("sword"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(item("TFT_Item_Sword", "B.F. Sword")),
                        PageRequest.of(1, 6),
                        13
                ));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "items",
                null,
                " Sword ",
                2,
                6,
                null,
                null,
                null
        );

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(guideItemRepository).searchPage(eq("17.0"), eq("sword"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(6);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(6);
        assertThat(response.getTotalItems()).isEqualTo(13);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    void 한_분할_테이블에_더_높은_버전이_있어도_카탈로그는_ACTIVE_스냅샷을_사용한다() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));
        lenient().when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(trait("TFT17_AnimalSquad", "Animal Squad", "[{\"name\":\"Briar\"}]")));
        when(guideItemRepository.findByPatchVersionOrderByNameAscIdAsc("17.0")).thenReturn(List.of());
        when(guideAugmentRepository.findByPatchVersionOrderByNameAscIdAsc("17.0")).thenReturn(List.of());
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.0")).thenReturn(List.of());

        // when
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.0");
        assertThat(response.getEntries())
                .extracting(GuideEntryResponse::getTargetKey)
                .containsExactly("TFT17_AnimalSquad");
        verify(guideTraitRepository, never()).findLatestPatchVersion();
    }

    @Test
    void 현재_패치_버전은_ACTIVE_스냅샷을_사용하고_카탈로그를_읽지_않는다() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));

        // when
        var response = guideService.getCurrentPatchVersion();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.0");
        verify(guideTraitRepository, never()).findByPatchVersionOrderByNameAscIdAsc(anyString());
        verify(guideChampionRepository, never()).findByPatchVersionOrderByNameAscIdAsc(anyString());
    }

    @Test
    void ACTIVE_스냅샷이_없으면_현재_패치_버전은_빈_문자열이다() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        var response = guideService.getCurrentPatchVersion();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("");
    }

    @Test
    void 검증_시각이_없는_ACTIVE_스냅샷은_현재_버전으로_공개하지_않는다() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.1", GuideSnapshotStatus.ACTIVE, null)));

        // when
        var response = guideService.getCurrentPatchVersion();

        // then
        assertThat(response.getPatchVersion()).isEmpty();
    }

    @Test
    void split_trait_response_skips_entries_without_champions() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));
        when(guideTraitRepository.countStargazerVariantsByPatchVersion("17.0")).thenReturn(0L);
        when(guideTraitRepository.searchPage(eq("17.0"), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(
                        trait("TFT17_AnimalSquad", "Animal Squad", "[{\"name\":\"Briar\"}]"),
                        trait("TFT17_DivineBlessing", "Divine Blessing", "[]")
                        ),
                        PageRequest.of(0, 10),
                        2
                ));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "traits",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems())
                .extracting(GuideEntryResponse::getTargetKey)
                .containsExactly("TFT17_AnimalSquad");
    }

    @Test
    void split_champion_response_skips_entries_without_traits() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(snapshot("17.0", GuideSnapshotStatus.ACTIVE, VALIDATED_AT)));
        when(guideChampionRepository.searchPage(eq("17.0"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(
                        champion("TFT17_DarkStar_FakeUnit", "Black Hole", 1, "[]"),
                        champion("TFT17_Briar", "Briar", 1, "[\"Animal Squad\"]")
                        ),
                        PageRequest.of(0, 10),
                        2
                ));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems())
                .extracting(GuideEntryResponse::getTargetKey)
                .containsExactly("TFT17_Briar");
    }

    @Test
    void STAGING_패치를_명시하면_공개하지_않고_빈_페이지를_반환한다() {
        // given
        when(guideSnapshotRepository.findByPatchVersion("17.1"))
                .thenReturn(Optional.of(snapshot("17.1", GuideSnapshotStatus.STAGING, null)));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "champions",
                "17.1",
                null,
                1,
                10,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
        assertThat(response.getTotalPages()).isEqualTo(1);
        verify(guideChampionRepository, never())
                .searchPage(anyString(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void 검증되지_않은_INACTIVE_패치를_명시하면_빈_페이지를_반환한다() {
        // given
        when(guideSnapshotRepository.findByPatchVersion("17.1"))
                .thenReturn(Optional.of(snapshot("17.1", GuideSnapshotStatus.INACTIVE, null)));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "items",
                "17.1",
                null,
                2,
                6,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(6);
        verify(guideItemRepository, never()).searchPage(anyString(), isNull(), any(Pageable.class));
    }

    @Test
    void ACTIVE_스냅샷이_없으면_카탈로그와_탭은_기존_빈_응답을_유지한다() {
        // given
        when(guideSnapshotRepository.findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when
        GuideCatalogResponse catalog = guideService.getGuideCatalog();
        GuidePageResponse<GuideEntryResponse> page = guideService.getGuideTabItems(
                "traits",
                null,
                null,
                1,
                6,
                null,
                null,
                null
        );

        // then
        assertThat(catalog.getPatchVersion()).isEmpty();
        assertThat(catalog.getEntries()).isEmpty();
        assertThat(page.getItems()).isEmpty();
        assertThat(page.getTotalPages()).isEqualTo(1);
        verify(guideTraitRepository, never())
                .searchPage(anyString(), isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void sort_parameter_throws_exception_until_metric_sort_is_supported() {
        // given, when, then
        assertThatThrownBy(() -> guideService.getGuideTabItems(
                "champions",
                "17.0",
                null,
                1,
                10,
                "avgPlace",
                "desc",
                null
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void invalid_tab_throws_exception() {
        // given, when, then
        assertThatThrownBy(() -> guideService.getGuideTabItems(
                "unknown",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        )).isInstanceOf(BusinessException.class);
    }

    private GuideChampion champion(String championKey, String name, int cost, String traitsJson) {
        return GuideChampion.builder()
                .championKey(championKey)
                .name(name)
                .cost(cost)
                .role("Carry")
                .position("Back")
                .imageUrl("https://example.com/" + championKey + ".png")
                .statsJson("{\"hp\":700,\"ad\":40}")
                .traitsJson(traitsJson)
                .bestItemsJson("[]")
                .patchVersion("17.0")
                .build();
    }

    private GuideTrait trait(String traitKey, String name, String championsJson) {
        return GuideTrait.builder()
                .traitKey(traitKey)
                .name(name)
                .type("Synergy")
                .iconUrl("https://example.com/" + traitKey + ".png")
                .tone("gold")
                .summary(name + " summary")
                .levelsJson("[\"2\"]")
                .tierEffectsJson("[{\"level\":\"2\",\"description\":\"effect\"}]")
                .championsJson(championsJson)
                .specialUnitsJson("[]")
                .tipsJson("[]")
                .patchVersion("17.0")
                .build();
    }

    private GuideItem item(String itemKey, String name) {
        return GuideItem.builder()
                .itemKey(itemKey)
                .name(name)
                .category("completed")
                .imageUrl("https://example.com/" + itemKey + ".png")
                .description(name + " description")
                .statsJson("{}")
                .bestUsersJson("[]")
                .combinationsJson("[]")
                .patchVersion("17.0")
                .build();
    }

    private GuideSnapshot snapshot(
            String patchVersion,
            GuideSnapshotStatus status,
            LocalDateTime validatedAt
    ) {
        return GuideSnapshot.builder()
                .patchVersion(patchVersion)
                .status(status)
                .validatedAt(validatedAt)
                .build();
    }
}
