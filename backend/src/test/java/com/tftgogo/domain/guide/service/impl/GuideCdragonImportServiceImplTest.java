package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideCdragonImportServiceImplTest {

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
    void cdragon_import_saves_champion_and_trait_to_split_tables() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, true, false, false));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(2);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getChampionCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);

        ArgumentCaptor<GuideChampion> championCaptor = ArgumentCaptor.forClass(GuideChampion.class);
        verify(guideChampionRepository).save(championCaptor.capture());
        GuideChampion champion = championCaptor.getValue();
        assertThat(champion.getChampionKey()).isEqualTo("TFT17_Briar");
        assertThat(champion.getPatchVersion()).isEqualTo("17.3");
        assertThat(champion.getTraitsJson()).contains("Animal Squad");

        ArgumentCaptor<GuideTrait> traitCaptor = ArgumentCaptor.forClass(GuideTrait.class);
        verify(guideTraitRepository).save(traitCaptor.capture());
        GuideTrait trait = traitCaptor.getValue();
        assertThat(trait.getTraitKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(trait.getPatchVersion()).isEqualTo("17.3");
        assertThat(trait.getChampionsJson()).contains("Briar");
    }

    @Test
    void cdragon_import_saves_item_without_stats_aggregation_values() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, true, false));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getItemCount()).isEqualTo(1);

        ArgumentCaptor<GuideItem> itemCaptor = ArgumentCaptor.forClass(GuideItem.class);
        verify(guideItemRepository).save(itemCaptor.capture());
        GuideItem item = itemCaptor.getValue();
        assertThat(item.getItemKey()).isEqualTo("TFT_Item_GuinsoosRageblade");
        assertThat(item.getStatsJson()).isEqualTo("{}");
        assertThat(item.getBestUsersJson()).isEqualTo("[]");
        assertThat(item.getCombinationsJson()).contains("Recurve Bow");
    }

    @Test
    void cdragon_import_saves_augment_without_stats_aggregation_values() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, false, true));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getAugmentCount()).isEqualTo(1);

        ArgumentCaptor<GuideAugment> augmentCaptor = ArgumentCaptor.forClass(GuideAugment.class);
        verify(guideAugmentRepository).save(augmentCaptor.capture());
        GuideAugment augment = augmentCaptor.getValue();
        assertThat(augment.getAugmentKey()).isEqualTo("TFT17_Augment_BattleReady");
        assertThat(augment.getStatsJson()).isEqualTo("{}");
        assertThat(augment.getTagsJson()).contains("Combat");
    }

    @Test
    void cdragon_import_updates_existing_split_row() {
        // given
        GuideChampion existingChampion = GuideChampion.builder()
                .championKey("TFT17_Briar")
                .name("Old Briar")
                .cost(1)
                .role("Old")
                .position("Old")
                .imageUrl("https://example.com/old.png")
                .statsJson("{}")
                .traitsJson("[]")
                .bestItemsJson("[]")
                .patchVersion("17.3")
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideChampionRepository.findByChampionKeyAndPatchVersion("TFT17_Briar", "17.3"))
                .thenReturn(Optional.of(existingChampion));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false, false, false));

        // then
        assertThat(response.getCreatedCount()).isZero();
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(existingChampion.getName()).isEqualTo("Briar");
        assertThat(existingChampion.getTraitsJson()).contains("Animal Squad");
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void request_without_include_targets_throws_exception() {
        // given, when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(false, false, false, false)))
                .isInstanceOf(BusinessException.class);
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
                          "name": "Briar",
                          "cost": 1,
                          "role": "ADCarry",
                          "squareIcon": "ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex",
                          "traits": ["Animal Squad"],
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
                            "name": "Bite",
                            "desc": "Deal @Damage@ damage.",
                            "effects": {
                              "Damage": 120
                            },
                            "icon": "ASSETS/Characters/TFT17_Briar/HUD/TFT17_Briar_Spell.tex"
                          }
                        }
                      ],
                      "traits": [
                        {
                          "apiName": "TFT17_AnimalSquad",
                          "name": "Animal Squad",
                          "desc": "Your team gains @TeamwideAD*100@% Attack Damage.<br><row>(@MinUnits@) @AttackSpeedPercent*100@% %i:scaleAS%</row>",
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
                            }
                          ]
                        }
                      ],
                      "augments": [
                        "TFT17_Augment_BattleReady"
                      ]
                    }
                  ],
                  "sets": {},
                  "items": [
                    {
                      "apiName": "TFT17_Augment_BattleReady",
                      "name": "Battle Ready",
                      "desc": "Your team gains @AttackSpeed@ attack speed.",
                      "effects": {
                        "AttackSpeed": 25
                      },
                      "icon": "ASSETS/UX/Augments/Battle_Ready.tex",
                      "tags": ["Combat"]
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
                      "desc": "Gain @AttackSpeed@ attack speed.",
                      "effects": {
                        "AttackSpeed": 10
                      },
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/GuinsoosRageblade.png",
                      "composition": ["TFT_Item_RecurveBow", "TFT_Item_NeedlesslyLargeRod"],
                      "associatedTraits": []
                    }
                  ]
                }
                """;
    }
}
