package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
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
        assertThat(championGuide.getDataJson()).contains("\"cost\":1");
        assertThat(championGuide.getDataJson()).contains("\"traits\":[\"동물특공대\"]");

        Guide traitGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.TRAIT)
                .findFirst()
                .orElseThrow();
        assertThat(traitGuide.getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(traitGuide.getDataJson()).contains("\"tone\":\"gold\"");
        assertThat(traitGuide.getDataJson()).contains("\"champions\":[{\"cost\":1");
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
        assertThat(itemGuide.getDataJson()).contains("\"label\":\"조합식\"");
        assertThat(itemGuide.getDataJson()).contains("Recurve Bow");
        assertThat(itemGuide.getDataJson()).contains("Needlessly Large Rod");
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

    private GuideCdragonImportRequest request(boolean includeChampions, boolean includeTraits) {
        return request(includeChampions, includeTraits, false);
    }

    private GuideCdragonImportRequest request(boolean includeChampions, boolean includeTraits, boolean includeItems) {
        GuideCdragonImportRequest request = new GuideCdragonImportRequest();
        ReflectionTestUtils.setField(request, "patchVersion", "17.3");
        ReflectionTestUtils.setField(request, "setNumber", 17);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet17");
        ReflectionTestUtils.setField(request, "includeChampions", includeChampions);
        ReflectionTestUtils.setField(request, "includeTraits", includeTraits);
        ReflectionTestUtils.setField(request, "includeItems", includeItems);
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
                            "desc": "<magicDamage>대상</magicDamage>에게 @Damage@ 피해를 입힙니다.",
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
                          "desc": "아군이 <b>공격력</b>을 얻습니다.",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_AnimalSquad.TFT_Set17.tex",
                          "effects": [
                            {"minUnits": 2, "maxUnits": 3, "style": 1},
                            {"minUnits": 4, "maxUnits": 25000, "style": 3}
                          ]
                        }
                      ]
                    }
                  ],
                  "sets": {},
                  "items": [
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
}
