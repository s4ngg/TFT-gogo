package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.GuideAugment;
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
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.config.GuideCdragonImportProperties;
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

import java.time.LocalDateTime;
import java.util.List;
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
    private GuideSnapshotRepository guideSnapshotRepository;

    @Mock
    private PatchNoteRepository patchNoteRepository;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private CommunityDragonProperties communityDragonProperties = new CommunityDragonProperties();

    @Spy
    private GuideCdragonImportProperties guideCdragonImportProperties = new GuideCdragonImportProperties();

    @InjectMocks
    private GuideCdragonImportServiceImpl guideCdragonImportService;

    @BeforeEach
    void setUp() {
        communityDragonProperties.setTftKoKrUrl("https://example.com/cdragon/tft/ko_kr.json");
        communityDragonProperties.setAssetBaseUrl("https://raw.communitydragon.org/latest/game");
        guideCdragonImportProperties.setMinimumChampionCount(1);
        guideCdragonImportProperties.setMinimumTraitCount(1);
        guideCdragonImportProperties.setMinimumItemCount(1);
        guideCdragonImportProperties.setMinimumAugmentCount(1);
        lenient().when(guideChampionRepository.findByChampionKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideTraitRepository.findByTraitKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideItemRepository.findByItemKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideAugmentRepository.findByAugmentKeyAndPatchVersion(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(guideSnapshotRepository.findByPatchVersionForUpdate(any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void 완전한_가져오기는_기존_스냅샷을_비활성화하고_새_스냅샷을_활성화한다() {
        // given
        GuideSnapshot previousActive = GuideSnapshot.builder()
                .patchVersion("17.2")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(1)
                .traitCount(1)
                .itemCount(1)
                .augmentCount(1)
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(previousActive));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, true, true, true));

        // then
        assertThat(response.getChampionCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);
        assertThat(response.getItemCount()).isEqualTo(1);
        assertThat(response.getAugmentCount()).isEqualTo(1);
        assertThat(response.getSetNumber()).isEqualTo(17);
        assertThat(response.getMutator()).isEqualTo("TFTSet17");
        assertThat(previousActive.getStatus()).isEqualTo(GuideSnapshotStatus.INACTIVE);

        ArgumentCaptor<GuideSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GuideSnapshot.class);
        verify(guideSnapshotRepository).save(snapshotCaptor.capture());
        GuideSnapshot activatedSnapshot = snapshotCaptor.getValue();
        assertThat(activatedSnapshot.getPatchVersion()).isEqualTo("17.3");
        assertThat(activatedSnapshot.getSourceSetNumber()).isEqualTo(17);
        assertThat(activatedSnapshot.getSourceMutator()).isEqualTo("TFTSet17");
        assertThat(activatedSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
        assertThat(activatedSnapshot.getChampionCount()).isEqualTo(1);
        assertThat(activatedSnapshot.getTraitCount()).isEqualTo(1);
        assertThat(activatedSnapshot.getItemCount()).isEqualTo(1);
        assertThat(activatedSnapshot.getAugmentCount()).isEqualTo(1);
        assertThat(activatedSnapshot.getValidatedAt()).isNotNull();
        assertThat(activatedSnapshot.getActivatedAt()).isNotNull();
        verify(guideSnapshotRepository).flush();
    }

    @Test
    void 현재_ACTIVE_패치를_완전_재수집하면_활성_상태를_유지하고_검증_건수를_갱신한다() {
        // given
        LocalDateTime activatedAt = LocalDateTime.of(2026, 7, 14, 8, 1);
        GuideSnapshot currentActive = GuideSnapshot.builder()
                .patchVersion("17.3")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(40)
                .traitCount(20)
                .itemCount(30)
                .augmentCount(50)
                .validatedAt(LocalDateTime.of(2026, 7, 14, 8, 0))
                .activatedAt(activatedAt)
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findByPatchVersionForUpdate("17.3"))
                .thenReturn(Optional.of(currentActive));
        when(guideSnapshotRepository.findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(currentActive));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, true, true, true));

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.3");
        assertThat(currentActive.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
        assertThat(currentActive.getChampionCount()).isEqualTo(1);
        assertThat(currentActive.getTraitCount()).isEqualTo(1);
        assertThat(currentActive.getItemCount()).isEqualTo(1);
        assertThat(currentActive.getAugmentCount()).isEqualTo(1);
        assertThat(currentActive.getValidatedAt()).isAfter(LocalDateTime.of(2026, 7, 14, 8, 0));
        assertThat(currentActive.getActivatedAt()).isEqualTo(activatedAt);
        verify(guideSnapshotRepository).save(currentActive);
        verify(guideSnapshotRepository, never()).flush();
    }

    @Test
    void 두_자리_마이너_패치는_숫자로_비교해_17_10이_17_9를_대체한다() {
        // given
        GuideSnapshot previousActive = GuideSnapshot.builder()
                .patchVersion("17.9")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(1)
                .traitCount(1)
                .itemCount(1)
                .augmentCount(1)
                .validatedAt(LocalDateTime.of(2026, 7, 14, 8, 0))
                .activatedAt(LocalDateTime.of(2026, 7, 14, 8, 1))
                .build();
        GuideCdragonImportRequest request = request(true, true, true, true);
        ReflectionTestUtils.setField(request, "patchVersion", "17.10");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(previousActive));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request);

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.10");
        assertThat(previousActive.getStatus()).isEqualTo(GuideSnapshotStatus.INACTIVE);
        ArgumentCaptor<GuideSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GuideSnapshot.class);
        verify(guideSnapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getPatchVersion()).isEqualTo("17.10");
        assertThat(snapshotCaptor.getValue().getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
        verify(guideSnapshotRepository).flush();
    }

    @Test
    void 과거_패치의_완전한_가져오기는_검증된_히스토리로만_저장한다() {
        // given
        GuideSnapshot currentActive = GuideSnapshot.builder()
                .patchVersion("17.3")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(1)
                .traitCount(1)
                .itemCount(1)
                .augmentCount(1)
                .validatedAt(LocalDateTime.of(2026, 7, 14, 8, 0))
                .activatedAt(LocalDateTime.of(2026, 7, 14, 8, 1))
                .build();
        GuideCdragonImportRequest request = request(true, true, true, true);
        ReflectionTestUtils.setField(request, "patchVersion", "17.2");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus.ACTIVE))
                .thenReturn(Optional.of(currentActive));

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request);

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.2");
        assertThat(currentActive.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
        ArgumentCaptor<GuideSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GuideSnapshot.class);
        verify(guideSnapshotRepository).save(snapshotCaptor.capture());
        GuideSnapshot historicalSnapshot = snapshotCaptor.getValue();
        assertThat(historicalSnapshot.getPatchVersion()).isEqualTo("17.2");
        assertThat(historicalSnapshot.getSourceSetNumber()).isEqualTo(17);
        assertThat(historicalSnapshot.getSourceMutator()).isEqualTo("TFTSet17");
        assertThat(historicalSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.INACTIVE);
        assertThat(historicalSnapshot.getValidatedAt()).isNotNull();
        assertThat(historicalSnapshot.getActivatedAt()).isNull();
        verify(guideSnapshotRepository, never()).flush();
    }

    @Test
    void 완전한_가져오기에서_필수_종류가_0건이면_저장과_활성화를_하지_않는다() {
        // given
        GuideSnapshot previousActive = GuideSnapshot.builder()
                .patchVersion("17.2")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(1)
                .traitCount(1)
                .itemCount(1)
                .augmentCount(1)
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonSpecialUnitJson());

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(true, true, true, true)))
                .isInstanceOf(BusinessException.class);
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
        verify(guideTraitRepository, never()).save(any(GuideTrait.class));
        verify(guideItemRepository, never()).save(any(GuideItem.class));
        verify(guideAugmentRepository, never()).save(any(GuideAugment.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
        verify(guideSnapshotRepository, never()).flush();
        assertThat(previousActive.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
    }

    @Test
    void 부분_가져오기에서_선택한_종류가_0건이면_저장하지_않는다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonSpecialUnitJson());

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(false, false, false, true)))
                .isInstanceOf(BusinessException.class);
        verify(guideAugmentRepository, never()).save(any(GuideAugment.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
    }

    @Test
    void 완전한_가져오기에서_설정된_최소_건수보다_적으면_저장하지_않는다() {
        // given
        GuideSnapshot previousActive = GuideSnapshot.builder()
                .patchVersion("17.2")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(10)
                .traitCount(10)
                .itemCount(10)
                .augmentCount(10)
                .build();
        guideCdragonImportProperties.setMinimumChampionCount(2);
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(true, true, true, true)))
                .isInstanceOf(BusinessException.class);
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
        verify(guideTraitRepository, never()).save(any(GuideTrait.class));
        verify(guideItemRepository, never()).save(any(GuideItem.class));
        verify(guideAugmentRepository, never()).save(any(GuideAugment.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
        verify(guideSnapshotRepository, never()).flush();
        assertThat(previousActive.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
    }

    @Test
    void 부분_가져오기는_데이터를_저장하지만_새_패치를_STAGING으로_유지한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, false, false, false));

        // then
        assertThat(response.getChampionCount()).isEqualTo(1);
        ArgumentCaptor<GuideSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GuideSnapshot.class);
        verify(guideSnapshotRepository).save(snapshotCaptor.capture());
        GuideSnapshot stagedSnapshot = snapshotCaptor.getValue();
        assertThat(stagedSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.STAGING);
        assertThat(stagedSnapshot.getSourceSetNumber()).isEqualTo(17);
        assertThat(stagedSnapshot.getSourceMutator()).isEqualTo("TFTSet17");
        assertThat(stagedSnapshot.getChampionCount()).isEqualTo(1);
        assertThat(stagedSnapshot.getTraitCount()).isZero();
        assertThat(stagedSnapshot.getItemCount()).isZero();
        assertThat(stagedSnapshot.getAugmentCount()).isZero();
        assertThat(stagedSnapshot.getValidatedAt()).isNull();
        assertThat(stagedSnapshot.getActivatedAt()).isNull();
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
    }

    @Test
    void 부분_가져오기가_현재_ACTIVE_패치를_대상으로_하면_저장_전에_거절한다() {
        // given
        LocalDateTime validatedAt = LocalDateTime.of(2026, 7, 13, 6, 40);
        LocalDateTime activatedAt = LocalDateTime.of(2026, 7, 13, 6, 41);
        GuideSnapshot activeSnapshot = GuideSnapshot.builder()
                .patchVersion("17.3")
                .status(GuideSnapshotStatus.ACTIVE)
                .championCount(4)
                .traitCount(5)
                .itemCount(6)
                .augmentCount(7)
                .validatedAt(validatedAt)
                .activatedAt(activatedAt)
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findByPatchVersionForUpdate("17.3"))
                .thenReturn(Optional.of(activeSnapshot));

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(true, false, false, false)))
                .isInstanceOf(BusinessException.class);
        assertThat(activeSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.ACTIVE);
        assertThat(activeSnapshot.getChampionCount()).isEqualTo(4);
        assertThat(activeSnapshot.getTraitCount()).isEqualTo(5);
        assertThat(activeSnapshot.getItemCount()).isEqualTo(6);
        assertThat(activeSnapshot.getAugmentCount()).isEqualTo(7);
        assertThat(activeSnapshot.getValidatedAt()).isEqualTo(validatedAt);
        assertThat(activeSnapshot.getActivatedAt()).isEqualTo(activatedAt);
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
    }

    @Test
    void 부분_가져오기가_검증된_INACTIVE_패치를_대상으로_하면_저장_전에_거절한다() {
        // given
        GuideSnapshot historicalSnapshot = GuideSnapshot.builder()
                .patchVersion("17.3")
                .status(GuideSnapshotStatus.INACTIVE)
                .championCount(40)
                .traitCount(20)
                .itemCount(30)
                .augmentCount(50)
                .validatedAt(LocalDateTime.of(2026, 7, 13, 6, 40))
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findByPatchVersionForUpdate("17.3"))
                .thenReturn(Optional.of(historicalSnapshot));

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request(false, false, true, false)))
                .isInstanceOf(BusinessException.class);
        assertThat(historicalSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.INACTIVE);
        assertThat(historicalSnapshot.getItemCount()).isEqualTo(30);
        verify(guideItemRepository, never()).save(any(GuideItem.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
    }

    @Test
    void 부분_가져오기는_기존_STAGING의_미선택_종류_건수를_보존한다() {
        // given
        GuideSnapshot stagedSnapshot = GuideSnapshot.builder()
                .patchVersion("17.3")
                .status(GuideSnapshotStatus.STAGING)
                .championCount(1)
                .traitCount(2)
                .itemCount(0)
                .augmentCount(3)
                .build();
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(guideSnapshotRepository.findByPatchVersionForUpdate("17.3"))
                .thenReturn(Optional.of(stagedSnapshot));

        // when
        guideCdragonImportService.importGuides(request(false, false, true, false));

        // then
        assertThat(stagedSnapshot.getStatus()).isEqualTo(GuideSnapshotStatus.STAGING);
        assertThat(stagedSnapshot.getSourceSetNumber()).isEqualTo(17);
        assertThat(stagedSnapshot.getSourceMutator()).isEqualTo("TFTSet17");
        assertThat(stagedSnapshot.getChampionCount()).isEqualTo(1);
        assertThat(stagedSnapshot.getTraitCount()).isEqualTo(2);
        assertThat(stagedSnapshot.getItemCount()).isEqualTo(1);
        assertThat(stagedSnapshot.getAugmentCount()).isEqualTo(3);
        assertThat(stagedSnapshot.getValidatedAt()).isNull();
        assertThat(stagedSnapshot.getActivatedAt()).isNull();
        verify(guideSnapshotRepository).save(stagedSnapshot);
        verify(guideSnapshotRepository, never()).findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
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
        assertThat(response.getPatchVersion()).isEqualTo("17.3");
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
        assertThat(response.getPatchVersion()).isEqualTo("17.3");
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
    void cdragon_import_expands_item_keyword_template_tokens() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonItemKeywordJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(false, false, true, false));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(1);
        ArgumentCaptor<GuideItem> itemCaptor = ArgumentCaptor.forClass(GuideItem.class);
        verify(guideItemRepository).save(itemCaptor.capture());
        GuideItem item = itemCaptor.getValue();
        assertThat(item.getItemKey()).isEqualTo("TFT_Item_InfinityEdge");
        assertThat(item.getDescription())
                .isEqualTo("정밀을 얻습니다. 정밀: 스킬과 아이템 피해가 치명타로 적용될 수 있습니다.");
        assertThat(item.getCombinationsJson()).contains("Sparring Gloves");
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
        assertThat(response.getPatchVersion()).isEqualTo("17.3");
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
    void latest_patch_version_uses_current_patch_note_version() {
        // given
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "patchVersion", "latest");
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(patchNote("17.5")));
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request);

        // then
        assertThat(response.getPatchVersion()).isEqualTo("17.5");
        ArgumentCaptor<GuideChampion> championCaptor = ArgumentCaptor.forClass(GuideChampion.class);
        verify(guideChampionRepository).save(championCaptor.capture());
        assertThat(championCaptor.getValue().getPatchVersion()).isEqualTo("17.5");
    }

    @Test
    void setNumber가_없으면_CDragon을_조회하지_않고_거절한다() {
        // given
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "setNumber", null);

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        verify(restTemplate, never()).getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
        verify(guideSnapshotRepository, never()).findByPatchVersionForUpdate(any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void mutator가_비어있으면_CDragon을_조회하지_않고_거절한다() {
        // given
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "mutator", "   ");

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        verify(restTemplate, never()).getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
        verify(guideSnapshotRepository, never()).findByPatchVersionForUpdate(any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void 명시한_현재_set만_사용하고_더_큰_미래_set은_선택하지_않는다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonMultipleSetsJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(
                request(true, false, false, false)
        );

        // then
        assertThat(response.getChampionCount()).isEqualTo(1);
        assertThat(response.getSetNumber()).isEqualTo(17);
        assertThat(response.getMutator()).isEqualTo("TFTSet17");
        ArgumentCaptor<GuideChampion> championCaptor = ArgumentCaptor.forClass(GuideChampion.class);
        verify(guideChampionRepository).save(championCaptor.capture());
        assertThat(championCaptor.getValue().getChampionKey()).isEqualTo("TFT17_CurrentCarry");
    }

    @Test
    void 신규_패치에서_운영_설정과_다른_미래_set은_저장하지_않는다() {
        // given
        guideCdragonImportProperties.setEnabled(true);
        guideCdragonImportProperties.setSetNumber(17);
        guideCdragonImportProperties.setMutator("TFTSet17");
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "patchVersion", "latest");
        ReflectionTestUtils.setField(request, "setNumber", 18);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet18");
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(patchNote("17.3")));

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        verify(restTemplate, never()).getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
        verify(guideSnapshotRepository, never()).findByPatchVersionForUpdate(any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void 운영_패치보다_미래인_패치와_set은_CDragon_조회_전에_거절한다() {
        // given
        guideCdragonImportProperties.setEnabled(true);
        guideCdragonImportProperties.setPatchVersion("latest");
        guideCdragonImportProperties.setSetNumber(17);
        guideCdragonImportProperties.setMutator("TFTSet17");
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "patchVersion", "17.4");
        ReflectionTestUtils.setField(request, "setNumber", 18);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet18");
        when(patchNoteRepository.findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc())
                .thenReturn(Optional.of(patchNote("17.3")));

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        verify(restTemplate, never()).getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
        verify(guideSnapshotRepository, never()).findByPatchVersionForUpdate(any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void 기존_스냅샷과_source가_다르면_분할_테이블_저장_전에_거절한다() {
        // given
        GuideSnapshot stagedSnapshot = GuideSnapshot.builder()
                .patchVersion("17.3")
                .sourceSetNumber(17)
                .sourceMutator("TFTSet17")
                .status(GuideSnapshotStatus.STAGING)
                .championCount(1)
                .traitCount(0)
                .itemCount(0)
                .augmentCount(0)
                .build();
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "setNumber", 18);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet18");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonMultipleSetsJson());
        when(guideSnapshotRepository.findByPatchVersionForUpdate("17.3"))
                .thenReturn(Optional.of(stagedSnapshot));

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        assertThat(stagedSnapshot.getSourceSetNumber()).isEqualTo(17);
        assertThat(stagedSnapshot.getSourceMutator()).isEqualTo("TFTSet17");
        assertThat(stagedSnapshot.getChampionCount()).isEqualTo(1);
        verify(guideChampionRepository, never()).findByChampionKeyAndPatchVersion(any(), any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
        verify(guideSnapshotRepository, never()).save(any(GuideSnapshot.class));
        verify(guideSnapshotRepository, never())
                .findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(any());
    }

    @Test
    void 비기본_mutator는_sets_폴백으로_대체하지_않는다() {
        // given
        GuideCdragonImportRequest request = request(true, false, false, false);
        ReflectionTestUtils.setField(request, "mutator", "TFTSet17_PBE");
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonFallbackSetJson());

        // when, then
        assertThatThrownBy(() -> guideCdragonImportService.importGuides(request))
                .isInstanceOf(BusinessException.class);
        verify(guideSnapshotRepository, never()).findByPatchVersionForUpdate(any());
        verify(guideChampionRepository, never()).save(any(GuideChampion.class));
    }

    @Test
    void cdragon_import_saves_special_units_on_trait_without_champion_row() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonSpecialUnitJson());

        // when
        GuideImportResponse response = guideCdragonImportService.importGuides(request(true, true, false, false));

        // then
        assertThat(response.getCreatedCount()).isEqualTo(2);
        assertThat(response.getChampionCount()).isEqualTo(1);
        assertThat(response.getTraitCount()).isEqualTo(1);

        ArgumentCaptor<GuideChampion> championCaptor = ArgumentCaptor.forClass(GuideChampion.class);
        verify(guideChampionRepository).save(championCaptor.capture());
        assertThat(championCaptor.getValue().getChampionKey()).isEqualTo("TFT17_Rhaast");

        ArgumentCaptor<GuideTrait> traitCaptor = ArgumentCaptor.forClass(GuideTrait.class);
        verify(guideTraitRepository).save(traitCaptor.capture());
        GuideTrait savedTrait = traitCaptor.getValue();
        assertThat(savedTrait.getTraitKey()).isEqualTo("TFT17_DarkStar");
        assertThat(savedTrait.getSpecialUnitsJson()).contains("Small Black Hole");
        assertThat(savedTrait.getSpecialUnitsJson()).contains("tft17_darkstar_fakeunit_smallsplash.tft_set17.png");
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

    private PatchNote patchNote(String version) {
        return PatchNote.builder()
                .version(version)
                .title(version + " patch")
                .summary("summary")
                .description("description")
                .publishedAt(LocalDateTime.of(2026, 6, 18, 9, 0))
                .current(true)
                .build();
    }

    private String cdragonMultipleSetsJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [
                        {
                          "apiName": "TFT17_CurrentCarry",
                          "name": "Current Carry",
                          "cost": 4,
                          "role": "ADCarry",
                          "squareIcon": "ASSETS/Characters/TFT17_CurrentCarry/HUD/TFT17_CurrentCarry_Square.TFT_Set17.tex",
                          "traits": ["Current Trait"],
                          "stats": {
                            "armor": 30,
                            "attackSpeed": 0.7,
                            "damage": 50,
                            "hp": 700,
                            "initialMana": 0,
                            "magicResist": 30,
                            "mana": 60,
                            "range": 4
                          }
                        }
                      ],
                      "traits": [],
                      "augments": []
                    },
                    {
                      "number": 18,
                      "mutator": "TFTSet18",
                      "champions": [
                        {
                          "apiName": "TFT18_TestCarry",
                          "name": "Test Carry",
                          "cost": 5,
                          "role": "APCarry",
                          "squareIcon": "ASSETS/Characters/TFT18_TestCarry/HUD/TFT18_TestCarry_Square.TFT_Set18.tex",
                          "traits": ["Next Trait"],
                          "stats": {
                            "armor": 35,
                            "attackSpeed": 0.8,
                            "damage": 60,
                            "hp": 800,
                            "initialMana": 20,
                            "magicResist": 35,
                            "mana": 80,
                            "range": 4
                          }
                        }
                      ],
                      "traits": [],
                      "augments": []
                    }
                  ],
                  "sets": {},
                  "items": []
                }
                """;
    }

    private String cdragonFallbackSetJson() {
        return """
                {
                  "setData": [],
                  "sets": {
                    "17": {
                      "champions": [
                        {
                          "apiName": "TFT17_CurrentCarry",
                          "name": "Current Carry",
                          "cost": 4,
                          "role": "ADCarry",
                          "traits": ["Current Trait"],
                          "stats": {
                            "range": 4
                          }
                        }
                      ],
                      "traits": []
                    }
                  },
                  "items": []
                }
                """;
    }

    private String cdragonSpecialUnitJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [
                        {
                          "apiName": "TFT17_Rhaast",
                          "name": "Rhaast",
                          "cost": 3,
                          "role": "APFighter",
                          "squareIcon": "ASSETS/Characters/TFT17_Rhaast/HUD/TFT17_Rhaast_Square.TFT_Set17.tex",
                          "traits": ["Dark Star"],
                          "stats": {
                            "armor": 45,
                            "attackSpeed": 0.75,
                            "damage": 55,
                            "hp": 850,
                            "initialMana": 20,
                            "magicResist": 45,
                            "mana": 70,
                            "range": 1
                          },
                          "ability": {
                            "name": "Shadow Assault",
                            "desc": "Deal @Damage@ damage.",
                            "effects": {
                              "Damage": 300
                            },
                            "icon": "ASSETS/Characters/TFT17_Rhaast/HUD/TFT17_Rhaast_Spell.tex"
                          }
                        },
                        {
                          "apiName": "TFT17_DarkStar_FakeUnit",
                          "name": "Small Black Hole",
                          "cost": 1,
                          "role": "APTank",
                          "squareIcon": "",
                          "traits": [],
                          "stats": {
                            "range": 1
                          }
                        }
                      ],
                      "traits": [
                        {
                          "apiName": "TFT17_DarkStar",
                          "name": "Dark Star",
                          "desc": "Dark Star creates a black hole in combat.<br><row>(@MinUnits@) Create @SmallBlackHoleCount@ small black hole</row>",
                          "icon": "ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex",
                          "effects": [
                            {
                              "minUnits": 3,
                              "maxUnits": 5,
                              "style": 1,
                              "variables": {
                                "SmallBlackHoleCount": 1
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

    private String cdragonItemKeywordJson() {
        return """
                {
                  "setData": [
                    {
                      "number": 17,
                      "mutator": "TFTSet17",
                      "champions": [],
                      "traits": [],
                      "augments": []
                    }
                  ],
                  "sets": {},
                  "items": [
                    {
                      "apiName": "TFT_Item_BFSword",
                      "name": "B.F. Sword",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/BFSword.png"
                    },
                    {
                      "apiName": "TFT_Item_SparringGloves",
                      "name": "Sparring Gloves",
                      "icon": "ASSETS/Maps/Particles/TFT/Item_Icons/Standard/SparringGloves.png"
                    },
                    {
                      "apiName": "TFT_Item_InfinityEdge",
                      "name": "무한의 대검",
                      "desc": "<TFTKeyword>정밀</TFTKeyword>을 얻습니다.<br><br>{{TFT_Keyword_Precision}}",
                      "effects": {
                        "AD": 0.35,
                        "CritChance": 35,
                        "CritDamageToGive": null
                      },
                      "icon": "ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex",
                      "composition": ["TFT_Item_BFSword", "TFT_Item_SparringGloves"],
                      "associatedTraits": []
                    }
                  ]
                }
                """;
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
