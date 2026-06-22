package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideCdragonImportServiceImplTest {

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
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private CommunityDragonProperties communityDragonProperties = new CommunityDragonProperties();

    @InjectMocks
    private GuideCdragonImportServiceImpl guideCdragonImportService;

    @BeforeEach
    void setUp() {
        communityDragonProperties.setTftKoKrUrl("https://example.com/cdragon/tft/ko_kr.json");
        communityDragonProperties.setAssetBaseUrl("https://raw.communitydragon.org/latest/game");
        lenient().when(guideChampionRepository.findByChampionKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideTraitRepository.findByTraitKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideItemRepository.findByItemKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideAugmentRepository.findByAugmentKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void CDragon_챔피언과_특성을_가이드로_생성한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(2);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getChampionCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getAugmentCount()).isZero();
        assertThat(response.getImportedCount()).isEqualTo(2);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository, times(2)).saveAndFlush(guideCaptor.capture());
        List<Guide> savedGuides = guideCaptor.getAllValues();

        Guide championGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.CHAMPION)
                .findFirst()
                .orElseThrow();
        assertThat(championGuide.getTargetKey()).isEqualTo("TFT17_Briar");
        assertThat(championGuide.getName()).isEqualTo("브라이어");
        assertThat(championGuide.getImageUrl()).contains("tft17_briar_splash_tile_10.tft_set17.png");
        assertThat(championGuide.getSummary()).contains("120");
        assertThat(championGuide.getSummary()).doesNotContain("@Damage@");
        assertThat(championGuide.getSummary()).doesNotContain("수치의");
        assertThat(championGuide.getSummary()).doesNotContain("수치개");
        assertThat(championGuide.getSummary()).doesNotContain("수치 얻");
        assertThat(championGuide.getSummary()).doesNotContain("수치일정");
        assertThat(championGuide.getSummary()).doesNotContain("수치 아래");
        assertThat(championGuide.getDataJson()).contains("\"cost\":1");
        assertThat(championGuide.getDataJson()).contains("\"traits\":[\"동물특공대\"]");
        assertThat(championGuide.getDataJson()).contains("120");
        assertThat(championGuide.getDataJson()).doesNotContain("@Damage@");
        assertThat(championGuide.getDataJson()).doesNotContain("수치의");
        assertThat(championGuide.getDataJson()).doesNotContain("수치개");
        assertThat(championGuide.getDataJson()).doesNotContain("수치 얻");
        assertThat(championGuide.getDataJson()).doesNotContain("수치일정");
        assertThat(championGuide.getDataJson()).doesNotContain("수치 아래");

        Guide traitGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.TRAIT)
                .findFirst()
                .orElseThrow();
        assertThat(traitGuide.getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(traitGuide.getSummary()).isEqualTo("아군이 공격력을 10% 얻습니다.");
        assertThat(traitGuide.getDataJson()).contains("\"tone\":\"gold\"");
        assertThat(traitGuide.getDataJson()).contains("\"summary\":\"아군이 공격력을 10% 얻습니다.\"");
        assertThat(traitGuide.getDataJson()).contains("\"tierEffects\":[{\"level\":\"2\",\"description\":\"15%\"},{\"level\":\"4+\",\"description\":\"30%\"}]");
        assertThat(traitGuide.getDataJson()).doesNotContain("%i:scaleAS%");
        assertThat(traitGuide.getDataJson()).doesNotContain("@TeamwideAD*100@");
        assertThat(traitGuide.getDataJson()).contains("\"champions\":[{\"cost\":1");
    }

    @Test
    void CDragon_챔피언이_없는_시너지는_가이드로_생성하지_않는다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonTraitWithNoChampionJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);
        assertThat(response.getImportedCount()).isEqualTo(1);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide savedGuide = guideCaptor.getValue();
        assertThat(savedGuide.getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(savedGuide.getDataJson()).contains("\"champions\":[{\"cost\":1");
        assertThat(savedGuide.getDataJson()).doesNotContain("TFT17_DivineBlessing");
    }

    @Test
    void CDragon_특성_summary는_ShowIfNot_비활성_문장을_제외하고_줄바꿈을_보존한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonConditionalTraitJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide traitGuide = guideCaptor.getValue();

        assertThat(traitGuide.getTargetKey()).isEqualTo("TFT17_DRX");
        assertThat(traitGuide.getSummary())
                .contains("아트록스: 아군이 입히는 피해가 적에게 30% 파쇄 및 파열 적용.")
                .contains("케이틀린: 아군 공격 속도 20% 증가");
        assertThat(traitGuide.getSummary()).doesNotContain("아트록스: 적 파쇄 및 파열 적용");
        assertThat(traitGuide.getSummary()).doesNotContain("케이틀린: 공격 속도 증가");
        assertThat(traitGuide.getDataJson()).contains("\"tierEffects\":[{\"level\":\"2+\",\"description\":\"전투 시작 6초 후 N.O.V.A. 유닛이 챔피언에 따라 아군에게 힘의 고조 부여\"}]");
    }

    @Test
    void CDragon_완성_아이템만_아이템_가이드로_생성한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getChampionCount()).isZero();
        assertThat(response.getTraitCount()).isZero();
        assertThat(response.getItemCount()).isEqualTo(1);
        assertThat(response.getAugmentCount()).isZero();
        assertThat(response.getImportedCount()).isEqualTo(1);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide itemGuide = guideCaptor.getValue();
        assertThat(itemGuide.getGuideType()).isEqualTo(GuideType.ITEM);
        assertThat(itemGuide.getTargetKey()).isEqualTo("TFT_Item_GuinsoosRageblade");
        assertThat(itemGuide.getName()).isEqualTo("Guinsoo's Rageblade");
        assertThat(itemGuide.getImageUrl()).contains("guinsoosrageblade.png");
        assertThat(itemGuide.getDataJson()).contains("\"category\":\"완성 아이템\"");
        assertThat(itemGuide.getDataJson()).contains("\"bestUsers\":[]");
        assertThat(itemGuide.getDataJson()).doesNotContain("bestUsersNote");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"top4\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"winRate\"");
        assertThat(itemGuide.getDataJson()).contains("\"label\":\"조합식\"");
        assertThat(itemGuide.getDataJson()).contains("Recurve Bow");
        assertThat(itemGuide.getDataJson()).contains("Needlessly Large Rod");
    }

    @Test
    void CDragon_아이템_import는_통계_필드를_생성하지_않는다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        guideCdragonImportService.importGuides(request(false, false, true));

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide itemGuide = guideCaptor.getValue();
        assertThat(itemGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"top4\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"winRate\"");
        assertThat(itemGuide.getDataJson()).contains("\"bestUsers\":[]");
    }

    @Test
    void CDragon_item_import_ignores_cached_match_stats() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        guideCdragonImportService.importGuides(request(false, false, true));

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide itemGuide = guideCaptor.getValue();
        assertThat(itemGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"top4\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"winRate\"");
    }

    @Test
    void CDragon_일반_증강체만_증강체_가이드로_생성한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getChampionCount()).isZero();
        assertThat(response.getTraitCount()).isZero();
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getAugmentCount()).isEqualTo(1);
        assertThat(response.getImportedCount()).isEqualTo(1);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide augmentGuide = guideCaptor.getValue();
        assertThat(augmentGuide.getGuideType()).isEqualTo(GuideType.AUGMENT);
        assertThat(augmentGuide.getTargetKey()).isEqualTo("TFT17_Augment_BattleReady");
        assertThat(augmentGuide.getName()).isEqualTo("Battle Ready");
        assertThat(augmentGuide.getImageUrl()).contains("battle_ready.png");
        assertThat(augmentGuide.getDataJson()).contains("\"description\":\"Your team gains 25 Attack Speed.\"");
        assertThat(augmentGuide.getDataJson()).doesNotContain("%i:scaleAS%");
        assertThat(augmentGuide.getDataJson()).contains("\"tier\":\"A\"");
        assertThat(augmentGuide.getDataJson()).contains("\"type\":\"Combat\"");
        assertThat(augmentGuide.getDataJson()).contains("\"reward\":\"전투 능력치\"");
        assertThat(augmentGuide.getDataJson()).contains("\"tags\":[\"전투\"]");
        assertThat(augmentGuide.getDataJson()).doesNotContain("{cf1fd3af}");
        assertThat(augmentGuide.getDataJson()).doesNotContain("@AttackSpeed@");
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"winRate\"");
    }

    @Test
    void CDragon_증강체_이름의_아이콘_토큰을_제거한다() {
        // given
        String iconTokenNameJson = cdragonJson()
                .replace("\"name\": \"Battle Ready\"", "\"name\": \"@Gold@골드 획득\"");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(iconTokenNameJson);
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide augmentGuide = guideCaptor.getValue();
        assertThat(augmentGuide.getName()).isEqualTo("골드 획득");
        assertThat(augmentGuide.getDataJson()).doesNotContain("@Gold@");
    }

    @Test
    void CDragon_증강체_티어가_없으면_UNKNOWN으로_저장한다() {
        // given
        String missingTierJson = cdragonJson()
                .replace("\"rarity\": \"gold\"", "\"rarity\": \"unknown\"");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(missingTierJson);
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide augmentGuide = guideCaptor.getValue();
        assertThat(augmentGuide.getDataJson()).contains("\"tier\":\"UNKNOWN\"");
    }

    @Test
    void CDragon_증강체_잘못된_곱셈_토큰은_import를_중단하지_않는다() {
        // given
        String invalidMultiplyTokenJson = cdragonJson()
                .replace("@AttackSpeed@ %i:scaleAS% Attack Speed", "@AttackSpeed*1.2.3@ Attack Speed");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(invalidMultiplyTokenJson);
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        assertThat(guideCaptor.getValue().getDataJson()).doesNotContain("@AttackSpeed*1.2.3@");
    }

    @Test
    void CDragon_증강체_import는_통계_필드를_생성하지_않는다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).saveAndFlush(guideCaptor.capture());
        Guide augmentGuide = guideCaptor.getValue();
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"winRate\"");
    }

    @Test
    void 이미_존재하는_가이드는_새로_생성하지_않고_수정한다() {
        // given
        Guide existingGuide = guide(GuideType.CHAMPION, "TFT17_Briar", "브라이어", "17.3");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(Optional.of(existingGuide));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false));

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(existingGuide.getSummary()).contains("물고기 광분");
        assertThat(existingGuide.getDataJson()).contains("\"ability\"");
        verify(guideRepository, never()).save(any(Guide.class));
    }

    @Test
    void 삭제된_가이드가_같은_키를_점유하면_생성하지_않고_스킵한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(true);

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false));

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getSkippedCount()).isEqualTo(1);
        verify(guideRepository, never()).saveAndFlush(any(Guide.class));
    }

    @Test
    void concurrent_create_conflict_updates_existing_active_guide() {
        // given
        Guide existingGuide = guide(GuideType.CHAMPION, "TFT17_Briar", "釉뚮씪?댁뼱", "17.3");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(Optional.empty(), Optional.of(existingGuide));
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate guide"))
                .when(guideRepository)
                .saveAndFlush(any(Guide.class));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false));

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isZero();
        assertThat(existingGuide.getDataJson()).contains("\"ability\"");
    }

    @Test
    void import_updates_existing_hidden_guide_without_publishing_it() {
        // given
        Guide hiddenGuide = guide(GuideType.CHAMPION, "TFT17_Briar", "TFT17_Briar", "17.3", false);
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                GuideType.CHAMPION,
                "TFT17_Briar",
                "17.3"
        )).thenReturn(Optional.of(hiddenGuide));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false));

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(hiddenGuide.isActive()).isFalse();
        assertThat(hiddenGuide.getDataJson()).contains("\"ability\"");
        verify(guideRepository, never()).saveAndFlush(any(Guide.class));
    }

    @Test
    void 요청한_세트가_CDragon에_없으면_INVALID_INPUT을_던진다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn("{\"setData\":[],\"sets\":{}}");

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(true, true)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void import_대상이_모두_false면_INVALID_INPUT을_던진다() {
        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(false, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(restTemplate, guideRepository);
    }

    @Test
    void 패치_버전이_20자를_넘으면_INVALID_INPUT을_던진다() {
        // given
        GuideCdragonImportRequest request = requestWithPatchVersion("123456789012345678901");

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(restTemplate, guideRepository);
    }

    @Test
    void CDragon_item_augment_import_uses_cdragon_without_cached_match_stats() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideRepository.findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(Optional.empty());
        when(guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                any(GuideType.class),
                any(String.class),
                any(String.class)
        )).thenReturn(false);
        when(guideRepository.saveAndFlush(any(Guide.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, true, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(2);
        assertThat(response.getItemCount()).isEqualTo(1);
        assertThat(response.getAugmentCount()).isEqualTo(1);
        assertThat(response.getImportedCount()).isEqualTo(2);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository, times(2)).saveAndFlush(guideCaptor.capture());
        List<Guide> savedGuides = guideCaptor.getAllValues();

        Guide itemGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.ITEM)
                .findFirst()
                .orElseThrow();
        assertThat(itemGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"top4\"");
        assertThat(itemGuide.getDataJson()).doesNotContain("\"winRate\"");

        Guide augmentGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.AUGMENT)
                .findFirst()
                .orElseThrow();
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"avgPlace\"");
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"pickRate\"");
        assertThat(augmentGuide.getDataJson()).doesNotContain("\"winRate\"");
    }

    private GuideCdragonImportRequest request(boolean includeChampions, boolean includeTraits) {
        return request(includeChampions, includeTraits, false);
    }

    private GuideCdragonImportRequest request(boolean includeChampions, boolean includeTraits, boolean includeItems) {
        return request(includeChampions, includeTraits, includeItems, false);
    }

    private GuideCdragonImportRequest request(
            boolean includeChampions,
            boolean includeTraits,
            boolean includeItems,
            boolean includeAugments
    ) {
        GuideCdragonImportRequest request = new GuideCdragonImportRequest();
        ReflectionTestUtils.setField(request, "patchVersion", "17.3");
        ReflectionTestUtils.setField(request, "setNumber", 17);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet17");
        ReflectionTestUtils.setField(request, "includeChampions", includeChampions);
        ReflectionTestUtils.setField(request, "includeTraits", includeTraits);
        ReflectionTestUtils.setField(request, "includeItems", includeItems);
        ReflectionTestUtils.setField(request, "includeAugments", includeAugments);
        return request;
    }

    private GuideCdragonImportRequest requestWithPatchVersion(String patchVersion) {
        GuideCdragonImportRequest request = request(true, true);
        ReflectionTestUtils.setField(request, "patchVersion", patchVersion);
        return request;
    }

    private Guide guide(GuideType guideType, String targetKey, String name, String patchVersion) {
        return guide(guideType, targetKey, name, patchVersion, true);
    }

    private Guide guide(GuideType guideType, String targetKey, String name, String patchVersion, boolean active) {
        return Guide.builder()
                .guideType(guideType)
                .targetKey(targetKey)
                .name(name)
                .summary("기존 요약")
                .imageUrl("https://example.com/old.png")
                .dataJson("{\"cost\":1}")
                .patchVersion(patchVersion)
                .sortOrder(0)
                .active(active)
                .build();
    }

    private String cdragonJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [
                        {
                          "apiName": "TFT17_Briar",
                          "name": "브라이어",
                          "cost": 1,
                          "role": "ADFighter",
                          "squareIcon": "ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex",
                          "traits": ["동물특공대"],
                          "stats": {
                            "armor": 35,
                            "attackSpeed": 0.75,
                            "damage": 35,
                            "hp": 650,
                            "initialMana": 0,
                            "magicResist": 35,
                            "mana": 40,
                            "range": 1
                          },
                          "ability": {
                            "name": "물고기 광분",
                            "desc": "<magicDamage>대상</magicDamage>에게 @Damage@ 피해를 입히고 @Shield@(%i:scaleAP%)의 보호막을 얻습니다. 미니 유성을 @MeteorCount@(%i:scaleAP%)개 떨어뜨려 각각 @SplashDamage@(%i:scaleAP%)의 마법 피해를 입힙니다. 공격 속도를 @Speed@(%i:scaleAS%) 얻습니다. 체력이 @Threshold@(%i:scaleHealth%) 아래인 적에게 @Bonus@(%i:scaleAP%)의 추가 마법 피해를 입힙니다. 다음 @Duration@(%i:scaleDuration%)일정 시간 동안 유지됩니다.",
                            "effects": {
                              "Damage": 120
                            },
                            "icon": "ASSETS/Characters/TFT17_Briar/HUD/TFT17_Briar_Spell.tex"
                          }
                        },
                        {
                          "apiName": "TFT_BlueGolem",
                          "name": "골렘",
                          "cost": 1,
                          "traits": []
                        }
                      ],
                      "traits": [
                        {
                          "apiName": "TFT17_AnimalSquad",
                          "name": "동물특공대",
                          "desc": "아군이 <b>공격력</b>을 @TeamwideAD*100@% 얻습니다.<br><row>(@MinUnits@) @AttackSpeedPercent*100@% %i:scaleAS%</row><br><row>(@MinUnits@) @AttackSpeedPercent*100@% %i:scaleAS%</row>",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_AnimalSquad.TFT_Set17.tex",
                          "effects": [
                            {
                              "minUnits": 2,
                              "maxUnits": 3,
                              "style": 1,
                              "variables": {
                                "TeamwideAD": 0.1,
                                "AttackSpeedPercent": 0.15
                              }
                            },
                            {
                              "minUnits": 4,
                              "maxUnits": 25000,
                              "style": 3,
                              "variables": {
                                "TeamwideAD": 0.1,
                                "AttackSpeedPercent": 0.3
                              }
                            }
                          ]
                        }
                      ],
                      "augments": [
                        "TFT17_Augment_BattleReady",
                        "TFT17_Augment_BattleReadyDuplicate",
                        "TFT17_Augment_DebugDummy"
                      ]
                    }
                  ],
                  "sets": {},
                  "items": [
                    {
                      "apiName": "TFT17_Augment_BattleReady",
                      "name": "Battle Ready",
                      "desc": "<rules>Your team gains @AttackSpeed@ %i:scaleAS% Attack Speed. () %</rules>",
                      "effects": {
                        "AttackSpeed": 25
                      },
                      "icon": "ASSETS/UX/Augments/Battle_Ready.tex",
                      "rarity": "gold",
                      "augmentType": "Combat",
                      "tags": ["{cf1fd3af}"]
                    },
                    {
                      "apiName": "TFT17_Augment_DebugDummy",
                      "name": "Debug Dummy",
                      "desc": "Test only",
                      "icon": "ASSETS/UX/Augments/Debug_Dummy.tex",
                      "rarity": "silver"
                    },
                    {
                      "apiName": "TFT17_Augment_BattleReadyDuplicate",
                      "name": "Battle Ready",
                      "desc": "<rules>Duplicate augment with the same display name.</rules>",
                      "effects": {},
                      "icon": "ASSETS/UX/Augments/Battle_Ready_Duplicate.tex",
                      "rarity": "gold",
                      "augmentType": "Combat",
                      "tags": ["{b72bd3bf}"]
                    },
                    {
                      "apiName": "TFT_Item_RecurveBow",
                      "name": "Recurve Bow",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/RecurveBow.png"
                    },
                    {
                      "apiName": "TFT_Item_NeedlesslyLargeRod",
                      "name": "Needlessly Large Rod",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/NeedlesslyLargeRod.png"
                    },
                    {
                      "apiName": "TFT_Item_GuinsoosRageblade",
                      "name": "Guinsoo's Rageblade",
                      "desc": "<stats>Gain @AttackSpeed@ Attack Speed.</stats>",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/GuinsoosRageblade.png",
                      "composition": ["TFT_Item_RecurveBow", "TFT_Item_NeedlesslyLargeRod"],
                      "associatedTraits": []
                    },
                    {
                      "apiName": "TFT_Item_CorruptedGuinsoosRageblade",
                      "name": "Guinsoo's Rageblade",
                      "desc": "<stats>Corrupted duplicate item.</stats>",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/GuinsoosRageblade.png",
                      "composition": ["TFT_Item_RecurveBow", "TFT_Item_NeedlesslyLargeRod"],
                      "associatedTraits": []
                    },
                    {
                      "apiName": "TFT_Item_AnimalSquadEmblem",
                      "name": "Animal Squad Emblem",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/AnimalSquadEmblem.png",
                      "composition": ["TFT_Item_Spatula", "TFT_Item_RecurveBow"],
                      "associatedTraits": ["TFT17_AnimalSquad"]
                    },
                    {
                      "apiName": "TFT5_Item_GuinsoosRagebladeRadiant",
                      "name": "Radiant Rageblade",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Radiant/RadiantRageblade.png"
                    }
                  ]
                }
                """;
    }

    private String cdragonTraitWithNoChampionJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [
                        {
                          "apiName": "TFT17_Briar",
                          "name": "브라이어",
                          "cost": 1,
                          "squareIcon": "ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex",
                          "traits": ["동물특공대"]
                        }
                      ],
                      "traits": [
                        {
                          "apiName": "TFT17_AnimalSquad",
                          "name": "동물특공대",
                          "desc": "아군이 공격력을 @TeamwideAD*100@% 얻습니다.<br><row>(@MinUnits@) @AttackSpeedPercent*100@%</row>",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_AnimalSquad.TFT_Set17.tex",
                          "effects": [
                            {
                              "minUnits": 2,
                              "maxUnits": 25000,
                              "style": 3,
                              "variables": {
                                "TeamwideAD": 0.1,
                                "AttackSpeedPercent": 0.15
                              }
                            }
                          ]
                        },
                        {
                          "apiName": "TFT17_DivineBlessing",
                          "name": "신의 축복",
                          "desc": "수치. 수치.",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_DivineBlessing.TFT_Set17.tex",
                          "effects": []
                        }
                      ],
                      "augments": []
                    }
                  ],
                  "sets": {},
                  "items": []
                }
                """;
    }

    private String cdragonConditionalTraitJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [
                        {
                          "apiName": "TFT17_Aatrox",
                          "name": "아트록스",
                          "cost": 1,
                          "squareIcon": "ASSETS/Characters/TFT17_Aatrox/Skins/Base/Images/TFT17_Aatrox_splash_tile_30.TFT_Set17.tex",
                          "traits": ["N.O.V.A."]
                        }
                      ],
                      "traits": [
                        {
                          "apiName": "TFT17_DRX",
                          "name": "N.O.V.A.",
                          "desc": "<row>(@MinUnits@) 전투 시작 @TeamAttackDelay@초 후 N.O.V.A. 유닛이 챔피언에 따라 아군에게 힘의 고조 부여</row><br><ShowIf.TFT17_DRX_HasAatrox><status>아트록스:</status> 아군이 입히는 피해가 적에게 @ShredAndSunder*100@% <TFTKeyword>파쇄</TFTKeyword> 및 <TFTKeyword>파열</TFTKeyword> 적용</ShowIf.TFT17_DRX_HasAatrox><ShowIfNot.TFT17_DRX_HasAatrox><TFTGuildInactive>아트록스: 적 파쇄 및 파열 적용</TFTGuildInactive></ShowIfNot.TFT17_DRX_HasAatrox><br><ShowIf.TFT17_DRX_HasCaitlyn><status>케이틀린:</status> 아군 공격 속도 @AS*100@% 증가</ShowIf.TFT17_DRX_HasCaitlyn><ShowIfNot.TFT17_DRX_HasCaitlyn><TFTGuildInactive>케이틀린: 공격 속도 증가</TFTGuildInactive></ShowIfNot.TFT17_DRX_HasCaitlyn>",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_Nova.TFT_Set17.tex",
                          "effects": [
                            {
                              "minUnits": 2,
                              "maxUnits": 25000,
                              "style": 5,
                              "variables": {
                                "AS": 0.2,
                                "ShredAndSunder": 0.3,
                                "TeamAttackDelay": 6
                              }
                            }
                          ]
                        }
                      ],
                      "augments": []
                    }
                  ],
                  "sets": {},
                  "items": []
                }
                """;
    }
}
