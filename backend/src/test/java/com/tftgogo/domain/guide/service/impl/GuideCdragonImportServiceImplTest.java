package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private CachedMatchRepository cachedMatchRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Mock
    private ResultSet tablesResultSet;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private CommunityDragonProperties communityDragonProperties = new CommunityDragonProperties();

    @InjectMocks
    private GuideCdragonImportServiceImpl guideCdragonImportService;

    @BeforeEach
    void setUp() throws SQLException {
        communityDragonProperties.setTftKoKrUrl("https://example.com/cdragon/tft/ko_kr.json");
        communityDragonProperties.setAssetBaseUrl("https://raw.communitydragon.org/latest/game");
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.getCatalog()).thenReturn("tftgogo");
        lenient().when(connection.getMetaData()).thenReturn(databaseMetaData);
        lenient().when(databaseMetaData.getTables(any(), any(), any(), any())).thenReturn(tablesResultSet);
        lenient().when(tablesResultSet.next()).thenReturn(true);
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
        assertThat(championGuide.getDataJson()).contains("\"cost\":1");
        assertThat(championGuide.getDataJson()).contains("\"traits\":[\"동물특공대\"]");

        Guide traitGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.TRAIT)
                .findFirst()
                .orElseThrow();
        assertThat(traitGuide.getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(traitGuide.getSummary()).isEqualTo("아군이 공격력을 10% 얻습니다. (2) 15% (4) 30%");
        assertThat(traitGuide.getDataJson()).contains("\"tone\":\"gold\"");
        assertThat(traitGuide.getDataJson()).contains("\"summary\":\"아군이 공격력을 10% 얻습니다. (2) 15% (4) 30%\"");
        assertThat(traitGuide.getDataJson()).doesNotContain("%i:scaleAS%");
        assertThat(traitGuide.getDataJson()).doesNotContain("@TeamwideAD*100@");
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
        assertThat(itemGuide.getDataJson()).contains("\"label\":\"조합식\"");
        assertThat(itemGuide.getDataJson()).contains("Recurve Bow");
        assertThat(itemGuide.getDataJson()).contains("Needlessly Large Rod");
    }

    @Test
    void CDragon_아이템_import는_cached_match_통계를_반영한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(cachedMatchRepository.findRecentByQueueIds(any(), any())).thenReturn(List.of(cachedMatch(cachedMatchJson())));
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
        assertThat(itemGuide.getDataJson()).contains("\"avgPlace\":\"3.00\"");
        assertThat(itemGuide.getDataJson()).contains("\"pickRate\":\"100.0%\"");
        assertThat(itemGuide.getDataJson()).contains("\"top4\":\"50.0%\"");
        assertThat(itemGuide.getDataJson()).contains("\"winRate\":\"50.0%\"");
        assertThat(itemGuide.getDataJson()).contains("\"bestUsers\":[{\"cost\":1");
        assertThat(itemGuide.getDataJson()).contains("\"name\":\"Briar\"");
        assertThat(itemGuide.getDataJson()).contains("tft17_briar_square.tft_set17.png");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cachedMatchRepository).findRecentByQueueIds(any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(500);
    }

    @Test
    void CDragon_item_import_skips_blank_cached_match_json() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(cachedMatchRepository.findRecentByQueueIds(any(), any())).thenReturn(List.of(cachedMatch("   ")));
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
        assertThat(itemGuide.getDataJson()).contains("\"avgPlace\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"pickRate\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"top4\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"winRate\":\"-\"");
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
        assertThat(augmentGuide.getDataJson()).contains("\"winRate\":\"-\"");
    }

    @Test
    void CDragon_증강체_import는_cached_match_통계를_반영한다() {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(cachedMatchRepository.findRecentByQueueIds(any(), any())).thenReturn(List.of(cachedMatch(cachedMatchJson())));
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
        assertThat(augmentGuide.getDataJson()).contains("\"avgPlace\":\"1.00\"");
        assertThat(augmentGuide.getDataJson()).contains("\"pickRate\":\"50.0%\"");
        assertThat(augmentGuide.getDataJson()).contains("\"winRate\":\"100.0%\"");
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
    void CDragon_item_augment_import_continues_when_cached_match_stats_unavailable() throws SQLException {
        // given
        when(restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class))
                .thenReturn(cdragonJson());
        when(tablesResultSet.next()).thenReturn(false);
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
        assertThat(itemGuide.getDataJson()).contains("\"avgPlace\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"pickRate\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"top4\":\"-\"");
        assertThat(itemGuide.getDataJson()).contains("\"winRate\":\"-\"");

        Guide augmentGuide = savedGuides.stream()
                .filter(guide -> guide.getGuideType() == GuideType.AUGMENT)
                .findFirst()
                .orElseThrow();
        assertThat(augmentGuide.getDataJson()).contains("\"avgPlace\":\"-\"");
        assertThat(augmentGuide.getDataJson()).contains("\"pickRate\":\"-\"");
        assertThat(augmentGuide.getDataJson()).contains("\"winRate\":\"-\"");

        verify(cachedMatchRepository, never()).findRecentByQueueIds(any(), any());
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

    private CachedMatch cachedMatch(String matchJson) {
        return CachedMatch.builder()
                .matchId("KR_1")
                .queueId(1100)
                .gameDatetime(1_717_200_000_000L)
                .matchJson(matchJson)
                .createdAt(LocalDateTime.now())
                .participantPuuids(Set.of("puuid-1", "puuid-2"))
                .build();
    }

    private String cachedMatchJson() {
        return """
                {
                  "info": {
                    "game_version": "Version 17.3.1234567",
                    "queue_id": 1100,
                    "participants": [
                      {
                        "puuid": "puuid-1",
                        "placement": 1,
                        "augments": ["TFT17_Augment_BattleReady"],
                        "units": [
                          {
                            "character_id": "TFT17_Briar",
                            "name": "Briar",
                            "rarity": 0,
                            "tier": 2,
                            "itemNames": ["TFT_Item_GuinsoosRageblade"]
                          }
                        ]
                      },
                      {
                        "puuid": "puuid-2",
                        "placement": 5,
                        "augments": ["TFT17_Augment_Other"],
                        "units": [
                          {
                            "character_id": "TFT17_Briar",
                            "name": "Briar",
                            "rarity": 0,
                            "tier": 2,
                            "itemNames": ["TFT_Item_GuinsoosRageblade"]
                          }
                        ]
                      }
                    ]
                  }
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
}
