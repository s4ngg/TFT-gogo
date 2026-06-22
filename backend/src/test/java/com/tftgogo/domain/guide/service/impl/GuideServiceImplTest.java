package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock
    private GuideRepository guideRepository;

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

    @BeforeEach
    void setUp() {
        lenient().when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        lenient().when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        lenient().when(guideItemRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        lenient().when(guideAugmentRepository.findLatestPatchVersion()).thenReturn(Optional.empty());
        lenient().when(guideChampionRepository.findByPatchVersionOrderByCostAscNameAscIdAsc(anyString()))
                .thenReturn(List.of());
        lenient().when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc(anyString()))
                .thenReturn(List.of());
        lenient().when(guideItemRepository.findByPatchVersionOrderByNameAscIdAsc(anyString()))
                .thenReturn(List.of());
        lenient().when(guideAugmentRepository.findByPatchVersionOrderByTierAscNameAscIdAsc(anyString()))
                .thenReturn(List.of());
    }

    @Test
    void 챔피언_탭은_cost_필터를_적용한다() {
        // given
        Guide fourCostChampion = championGuide("kaisa", "카이사", 4, 1);
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideRepository.findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, 4))
                .thenReturn(List.of(fourCostChampion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
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
        assertThat(response.getItems()).hasSize(1);
        verify(guideRepository).findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, 4);
    }

    @Test
    void dataJson은_JSON_object로_응답한다() {
        // given
        Guide champion = championGuide("kaisa", "카이사", 4, 1);
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideRepository.findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, null))
                .thenReturn(List.of(champion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
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
        Object firstItem = response.getItems().get(0);
        assertThat(firstItem)
                .hasFieldOrPropertyWithValue("name", "카이사")
                .extracting("dataJson")
                .satisfies(dataJson -> assertThat(dataJson.toString()).contains("\"cost\":4"));
    }

    @Test
    void 카탈로그는_최신_패치버전만_조회한다() {
        // given
        Guide latestChampion = championGuide("jinx", "징크스", 4, 1, "17.1");
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideRepository.findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1"))
                .thenReturn(List.of(latestChampion));

        // when
        List<GuideEntryResponse> response = guideService.getGuideCatalog();

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPatchVersion()).isEqualTo("17.1");
        verify(guideRepository)
                .findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1");
    }

    @Test
    void 명시한_패치버전은_최신_패치_조회없이_사용한다() {
        // given
        Guide champion = championGuide("kaisa", "카이사", 4, 1);
        when(guideRepository.findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, null))
                .thenReturn(List.of(champion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
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
        verify(guideRepository, never()).findLatestPatchVersion();
        verify(guideRepository).findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, null);
    }

    @Test
    void 검색어는_LIKE_와일드카드를_escape한다() {
        // given
        Guide champion = championGuide("kaisa", "카이사", 4, 1);
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideRepository.findFilteredGuides(
                GuideType.CHAMPION.name(),
                "17.0",
                "카이사\\_\\%\\\\",
                null
        )).thenReturn(List.of(champion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
                "champions",
                null,
                "카이사_%\\",
                1,
                10,
                null,
                null,
                null
        );

        // then
        assertThat(response.getItems()).hasSize(1);
        verify(guideRepository).findFilteredGuides(
                GuideType.CHAMPION.name(),
                "17.0",
                "카이사\\_\\%\\\\",
                null
        );
    }

    @Test
    void split_테이블_챔피언_탭은_cost_필터와_페이지네이션을_적용한다() {
        // given
        GuideChampion kaisa = splitChampionGuide("kaisa", "카이사", 4, "17.0");
        GuideChampion jinx = splitChampionGuide("jinx", "징크스", 4, "17.0");
        GuideChampion briar = splitChampionGuide("briar", "브라이어", 5, "17.0");
        when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideChampionRepository.findByPatchVersionOrderByCostAscNameAscIdAsc("17.0"))
                .thenReturn(List.of(kaisa, jinx, briar));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                2,
                1,
                null,
                null,
                4
        );

        // then
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0)).hasFieldOrPropertyWithValue("name", "징크스");
    }

    @Test
    void 탭_조회는_해당_split_테이블의_최신_패치버전을_우선한다() {
        // given
        GuideChampion champion = splitChampionGuide("kaisa", "카이사", 4, "17.0");
        when(guideChampionRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideChampionRepository.findByPatchVersionOrderByCostAscNameAscIdAsc("17.0"))
                .thenReturn(List.of(champion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
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
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0)).hasFieldOrPropertyWithValue("patchVersion", "17.0");
        verify(guideRepository, never()).findLatestPatchVersion();
    }

    @Test
    void 지원하지_않는_탭은_예외를_던진다() {
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

    @Test
    void 최대_페이지를_초과하면_예외를_던진다() {
        // given, when, then
        assertThatThrownBy(() -> guideService.getGuideTabItems(
                "champions",
                null,
                null,
                10_001,
                10,
                null,
                null,
                null
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void 통계_sortKey는_허용하지_않는다() {
        // given, when, then
        assertThatThrownBy(() -> guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                "top4",
                "desc",
                null
        )).isInstanceOf(BusinessException.class);
    }

    private Guide championGuide(String targetKey, String name, int cost, int sortOrder) {
        return championGuide(targetKey, name, cost, sortOrder, "17.0");
    }

    private Guide championGuide(String targetKey, String name, int cost, int sortOrder, String patchVersion) {
        return Guide.builder()
                .guideType(GuideType.CHAMPION)
                .targetKey(targetKey)
                .name(name)
                .summary(name + " 요약")
                .imageUrl("https://example.com/" + targetKey + ".png")
                .dataJson("{\"cost\":" + cost + ",\"role\":\"캐리\",\"traits\":[\"도전자\"],\"bestItems\":[],\"stats\":{}}")
                .patchVersion(patchVersion)
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private GuideChampion splitChampionGuide(String championKey, String name, int cost, String patchVersion) {
        return GuideChampion.builder()
                .championKey(championKey)
                .name(name)
                .cost(cost)
                .role("캐리")
                .position("중앙")
                .imageUrl("https://example.com/" + championKey + ".png")
                .statsJson("{}")
                .traitsJson("[\"도전자\"]")
                .bestItemsJson("[]")
                .patchVersion(patchVersion)
                .build();
    }
}
