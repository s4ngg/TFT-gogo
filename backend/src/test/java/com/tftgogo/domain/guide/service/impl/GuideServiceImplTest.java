package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.AugmentGuidePlan;
import com.tftgogo.domain.guide.entity.AugmentGuideReward;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.AugmentGuidePlanRepository;
import com.tftgogo.domain.guide.repository.AugmentGuideRewardRepository;
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

    @Mock
    private AugmentGuidePlanRepository augmentGuidePlanRepository;

    @Mock
    private AugmentGuideRewardRepository augmentGuideRewardRepository;

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
    void 카탈로그는_증강체_운영_플랜과_보상표를_함께_반환한다() {
        // given
        when(guideRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.1"));
        when(guideRepository.findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc("17.1"))
                .thenReturn(List.of());
        when(augmentGuidePlanRepository.findByPatchVersionOrderByPlanKeyAscIdAsc("17.1"))
                .thenReturn(List.of(augmentPlan("fast8", "빠른 8레벨", "17.1")));
        when(augmentGuideRewardRepository.findByPatchVersionOrderByStageAscIdAsc("17.1"))
                .thenReturn(List.of(augmentReward("2-1", "실버", "초반 전투 보강", "17.1")));

        // when
        GuideCatalogResponse response = guideService.getGuideCatalog();

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.1");
        assertThat(response.getAugmentPlans())
                .hasSize(1)
                .first()
                .satisfies(plan -> {
                    assertThat(plan.getKey()).isEqualTo("fast8");
                    assertThat(plan.getLabel()).isEqualTo("빠른 8레벨");
                    assertThat(plan.getStages()).hasSize(1);
                });
        assertThat(response.getRewards())
                .hasSize(1)
                .first()
                .satisfies(reward -> {
                    assertThat(reward.getStage()).isEqualTo("2-1");
                    assertThat(reward.getCondition()).isEqualTo("실버");
                    assertThat(reward.getReward()).isEqualTo("초반 전투 보강");
                });
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

    private AugmentGuidePlan augmentPlan(String planKey, String label, String patchVersion) {
        AugmentGuidePlan plan = instantiate(AugmentGuidePlan.class);
        ReflectionTestUtils.setField(plan, "id", 1L);
        ReflectionTestUtils.setField(plan, "planKey", planKey);
        ReflectionTestUtils.setField(plan, "label", label);
        ReflectionTestUtils.setField(
                plan,
                "stagesJson",
                "[{\"stage\":\"2-1\",\"choice\":\"전투 증강\",\"focus\":\"초반 전투력\"}]"
        );
        ReflectionTestUtils.setField(plan, "patchVersion", patchVersion);
        return plan;
    }

    private AugmentGuideReward augmentReward(
            String stage,
            String conditionText,
            String rewardText,
            String patchVersion
    ) {
        AugmentGuideReward reward = instantiate(AugmentGuideReward.class);
        ReflectionTestUtils.setField(reward, "id", 1L);
        ReflectionTestUtils.setField(reward, "stage", stage);
        ReflectionTestUtils.setField(reward, "conditionText", conditionText);
        ReflectionTestUtils.setField(reward, "rewardText", rewardText);
        ReflectionTestUtils.setField(reward, "patchVersion", patchVersion);
        return reward;
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
