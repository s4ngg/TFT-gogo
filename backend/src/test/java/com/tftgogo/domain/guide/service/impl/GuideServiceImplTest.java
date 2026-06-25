package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock
    private GuideChampionRepository guideChampionRepository;

    @Mock
    private GuideTraitRepository guideTraitRepository;

    @Mock
    private GuideItemRepository guideItemRepository;

    @Mock
    private GuideAugmentRepository guideAugmentRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GuideServiceImpl guideService;

    @Test
    void champion_tab_uses_split_table_page_and_applies_cost_filter() {
        // given
        when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
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
    void explicit_patch_does_not_query_latest_patch() {
        // given
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
        verify(guideChampionRepository, never()).findLatestPatchVersion();
    }

    @Test
    void tab_items_return_page_metadata_from_repository_page() {
        // given
        when(guideItemRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
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
    void catalog_uses_latest_patch_from_split_tables() {
        // given
        when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideItemRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        when(guideAugmentRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.1"))
                .thenReturn(List.of(trait("TFT17_AnimalSquad", "Animal Squad", "[{\"name\":\"Briar\"}]")));
        when(guideItemRepository.findByPatchVersionOrderByNameAscIdAsc("17.1")).thenReturn(List.of());
        when(guideAugmentRepository.findByPatchVersionOrderByNameAscIdAsc("17.1")).thenReturn(List.of());
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.1")).thenReturn(List.of());

        // when
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.1");
        assertThat(response.getEntries())
                .extracting(GuideEntryResponse::getTargetKey)
                .containsExactly("TFT17_AnimalSquad");
    }

    @Test
    void split_trait_response_skips_entries_without_champions() {
        // given
        when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideTraitRepository.existsStargazerVariantByPatchVersion("17.0")).thenReturn(false);
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
        when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
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
}
