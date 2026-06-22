package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getEntries()).hasSize(1);
        assertThat(response.getEntries().get(0).getPatchVersion()).isEqualTo("17.1");
        verify(guideRepository)
                .findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1");
    }

    @Test
    void 카탈로그는_증강체_운영_플랜을_반환하지_않는다() {
        // given
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideRepository.findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1"))
                .thenReturn(List.of());

        // when
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.1");
        assertThat(response.getEntries()).isEmpty();
    }

    @Test
    void 카탈로그_기본_패치는_가이드_데이터에서_선택한다() {
        // given
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideRepository.findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1"))
                .thenReturn(List.of());

        // when
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.1");
        verify(guideRepository).findLatestPatchVersion();
    }

    @Test
    void 분리_시너지_응답은_연결_챔피언이_없는_항목을_제외한다() {
        // given
        GuideTrait displayableTrait = traitGuide(
                "TFT17_AnimalSquad",
                "동물특공대",
                "[{\"cost\":1,\"name\":\"브라이어\",\"imageUrl\":\"https://example.com/briar.png\"}]"
        );
        GuideTrait hiddenTrait = traitGuide("TFT17_DivineBlessing", "신의 축복", "[]");
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(displayableTrait, hiddenTrait));

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
                .extracting(GuideEntryResponse::getName)
                .containsExactly("동물특공대");
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void stargazer_split_response_hides_base_trait_and_exposes_variant() {
        // given
        String championsJson = "[{\"cost\":3,\"name\":\"룰루\",\"imageUrl\":\"https://example.com/lulu.png\"}]";
        GuideTrait baseTrait = traitGuide("TFT17_Stargazer", "별돌보미", championsJson);
        GuideTrait variantTrait = traitGuide("TFT17_Stargazer_Huntress", "별돌보미", championsJson);
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(baseTrait, variantTrait));

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
                .containsExactly("TFT17_Stargazer_Huntress");
        assertThat(response.getItems().get(0).getDataJson().path("variant").asText())
                .isEqualTo("여사냥꾼");
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void split_champion_response_skips_entries_without_traits() {
        // given
        GuideChampion fakeUnit = splitChampionGuide("TFT17_DarkStar_FakeUnit", "Black Hole", "[]");
        GuideChampion shopChampion = splitChampionGuide("TFT17_Briar", "Briar", "[\"Animal Squad\"]");
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(fakeUnit, shopChampion));

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
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void 분리_챔피언_응답은_이름순으로_정렬한다() {
        // given
        GuideChampion jinx = splitChampionGuide("TFT17_Jinx", "징크스", "[\"별돌보미\"]");
        GuideChampion garen = splitChampionGuide("TFT17_Garen", "가렌", "[\"전략가\"]");
        GuideChampion ahri = splitChampionGuide("TFT17_Ahri", "아리", "[\"마법사\"]");
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(jinx, garen, ahri));

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
                .extracting(GuideEntryResponse::getName)
                .containsExactly("가렌", "아리", "징크스");
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
    void 분리_증강체_응답은_표시용_설명과_태그만_포함한다() {
        // given
        GuideAugment augment = augmentGuide(
                "TFT17_Augment_BattleReady",
                "전투 준비",
                "아군이 공격 속도를 얻습니다.",
                "[\"전투\"]",
                "{\"tier\":\"A\",\"type\":\"Combat\",\"reward\":\"전투 능력치\",\"winRate\":\"61.4%\"}",
                "17.0"
        );
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideAugmentRepository.findByPatchVersionOrderByNameAscIdAsc("17.0"))
                .thenReturn(List.of(augment));

        // when
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                "augments",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        );

        // then
        GuideEntryResponse firstItem = response.getItems().get(0);
        assertThat(firstItem.getImageUrl()).isEqualTo("https://example.com/TFT17_Augment_BattleReady.png");
        assertThat(firstItem.getDataJson().path("description").asText()).isEqualTo("아군이 공격 속도를 얻습니다.");
        assertThat(firstItem.getDataJson().path("tags").get(0).asText()).isEqualTo("전투");
        assertThat(firstItem.getDataJson().has("tier")).isFalse();
        assertThat(firstItem.getDataJson().has("type")).isFalse();
        assertThat(firstItem.getDataJson().has("reward")).isFalse();
        assertThat(firstItem.getDataJson().has("winRate")).isFalse();
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
    void 퍼센트_문자열_정렬은_공백과_기호를_허용한다() {
        // given
        Guide lowTop4Champion = championGuideWithTop4("kaisa", "카이사", "% 15 . 5", 1);
        Guide highTop4Champion = championGuideWithTop4("jinx", "징크스", "20.5%", 2);
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.0"));
        when(guideRepository.findFilteredGuides(GuideType.CHAMPION.name(), "17.0", null, null))
                .thenReturn(List.of(lowTop4Champion, highTop4Champion));

        // when
        GuidePageResponse<?> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                "top4",
                "desc",
                null
        );

        // then
        assertThat(response.getItems().get(0))
                .hasFieldOrPropertyWithValue("name", "징크스");
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

    private Guide championGuideWithTop4(String targetKey, String name, String top4, int sortOrder) {
        return Guide.builder()
                .guideType(GuideType.CHAMPION)
                .targetKey(targetKey)
                .name(name)
                .summary(name + " 요약")
                .imageUrl("https://example.com/" + targetKey + ".png")
                .dataJson("{\"cost\":4,\"top4\":\"" + top4 + "\",\"role\":\"캐리\",\"traits\":[\"도전자\"],"
                        + "\"bestItems\":[],\"stats\":{}}")
                .patchVersion("17.0")
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private GuideChampion splitChampionGuide(String championKey, String name, String traitsJson) {
        return GuideChampion.builder()
                .championKey(championKey)
                .name(name)
                .cost(1)
                .role("AP Tank")
                .position("Front")
                .imageUrl("https://example.com/" + championKey + ".png")
                .statsJson("{\"hp\":700,\"ad\":40,\"attackSpeed\":\"0.65\",\"mana\":\"0/50\",\"armor\":40,\"mr\":40}")
                .traitsJson(traitsJson)
                .bestItemsJson("[]")
                .patchVersion("17.0")
                .build();
    }

    private GuideTrait traitGuide(String traitKey, String name, String championsJson) {
        return GuideTrait.builder()
                .traitKey(traitKey)
                .name(name)
                .type("시너지")
                .iconUrl("https://example.com/" + traitKey + ".png")
                .tone("gold")
                .summary(name + " 요약")
                .levelsJson("[\"2\"]")
                .tierEffectsJson("[{\"level\":\"2\",\"description\":\"효과\"}]")
                .championsJson(championsJson)
                .tipsJson("[]")
                .patchVersion("17.0")
                .build();
    }

    private GuideAugment augmentGuide(
            String augmentKey,
            String name,
            String description,
            String tagsJson,
            String statsJson,
            String patchVersion
    ) {
        GuideAugment augment = GuideAugment.builder()
                .augmentKey(augmentKey)
                .name(name)
                .description(description)
                .iconUrl("https://example.com/" + augmentKey + ".png")
                .tagsJson(tagsJson)
                .statsJson(statsJson)
                .patchVersion(patchVersion)
                .build();
        ReflectionTestUtils.setField(augment, "id", 1L);
        return augment;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException
                 | InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException e) {
            throw new AssertionError("테스트 엔티티 생성 실패: " + type.getSimpleName(), e);
        }
    }
}
