package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository;
import com.tftgogo.domain.deck.repository.DeckCurationRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.global.riot.RiotApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDeckServiceImplTest {

    @Mock
    private MetaDeckRepository metaDeckRepository;

    @Mock
    private DeckCurationRepository deckCurationRepository;

    @Mock
    private ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;

    @Mock
    private RiotApiClient riotApiClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private AsyncAggregationRunner asyncAggregationRunner;

    @InjectMocks
    private MetaDeckServiceImpl metaDeckService;

    // ── getMetaDecks ────────────────────────────────────────────────────

    @Test
    void 집계된_데이터가_없으면_빈_덱_목록을_반환한다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of());

        // when
        MetaDeckListResponse response = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);

        // then
        assertThat(response.getDecks()).isEmpty();
        assertThat(response.getPatchVersion()).isNull();
    }

    @Test
    void 최신_패치버전_덱만_조회된다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.10", "16.11", "16.9"));
        when(metaDeckRepository.findMetaDecksByPickRateIn(eq(RankFilter.MASTER_PLUS), eq(List.of("16.11")), anyDouble()))
                .thenReturn(List.of(metaDeck("sig1", "16.11", "S", 5.0)));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of());

        // when
        MetaDeckListResponse response = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);

        // then
        assertThat(response.getPatchVersion()).isEqualTo("16.11");
        assertThat(response.getDecks()).hasSize(1);
    }

    @Test
    void 매핑이_적용되면_표시용_패치_번호로_조회된다() {
        // given: 서로 다른 client version(16.13, 16.14)이 같은 패치(17.6)로 매핑된 경우
        // meta_decks에는 원본 client version이 저장되어 있으므로 조회 시점에 매핑을 적용해 함께 조회해야 한다
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.13", "16.14"));
        when(clientVersionPatchMappingRepository.findAll()).thenReturn(List.of(
                com.tftgogo.domain.deck.entity.ClientVersionPatchMapping.builder()
                        .clientVersion("16.13").patchVersion("17.6").build(),
                com.tftgogo.domain.deck.entity.ClientVersionPatchMapping.builder()
                        .clientVersion("16.14").patchVersion("17.6").build()
        ));
        when(metaDeckRepository.findMetaDecksByPickRateIn(eq(RankFilter.MASTER_PLUS), any(), anyDouble()))
                .thenReturn(List.of(metaDeck("sig1", "16.13", "S", 5.0)));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of());

        // when
        MetaDeckListResponse response = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);

        // then: 응답의 patchVersion은 매핑된 표시값이고, 조회는 매핑된 원본 client version 둘 다를 대상으로 한다
        assertThat(response.getPatchVersion()).isEqualTo("17.6");
        verify(metaDeckRepository).findMetaDecksByPickRateIn(
                eq(RankFilter.MASTER_PLUS),
                argThat(rawVersions -> rawVersions.containsAll(List.of("16.13", "16.14")) && rawVersions.size() == 2),
                anyDouble());
    }

    @Test
    void 큐레이션_숨김_덱은_응답에서_제외된다() {
        // given
        MetaDeck hiddenDeck = metaDeck("hidden-sig", "16.11", "A", 3.0);
        MetaDeck visibleDeck = metaDeck("visible-sig", "16.11", "S", 5.0);

        DeckCuration hiddenCuration = DeckCuration.builder()
                .signature("hidden-sig")
                .rankFilter(RankFilter.MASTER_PLUS)
                .hidden(true)
                .build();

        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.11"));
        when(metaDeckRepository.findMetaDecksByPickRateIn(any(), any(), anyDouble()))
                .thenReturn(List.of(hiddenDeck, visibleDeck));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of(hiddenCuration));

        // when
        MetaDeckListResponse response = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);

        // then: 숨김 덱 제외, 보이는 덱만 포함
        assertThat(response.getDecks()).hasSize(1);
        assertThat(response.getDecks().get(0).getGrade()).isEqualTo("S");
    }

    @Test
    void 큐레이션_sortPriority가_있으면_우선_정렬된다() {
        // given
        MetaDeck highPlayRate = metaDeck("high-rate", "16.11", "S", 10.0);
        MetaDeck lowPlayRateWithPriority = metaDeck("low-rate-prio", "16.11", "B", 1.0);

        DeckCuration curation = DeckCuration.builder()
                .signature("low-rate-prio")
                .rankFilter(RankFilter.MASTER_PLUS)
                .hidden(false)
                .sortPriority(1)
                .build();

        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.11"));
        when(metaDeckRepository.findMetaDecksByPickRateIn(any(), any(), anyDouble()))
                .thenReturn(List.of(highPlayRate, lowPlayRateWithPriority));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of(curation));

        // when
        MetaDeckListResponse response = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);

        // then: sortPriority=1인 덱이 첫 번째 (grade=B)
        assertThat(response.getDecks().get(0).getGrade()).isEqualTo("B");
    }

    // ── aggregateAndSave (동시 집계 방지) ────────────────────────────────

    @Test
    void 집계_중_중복_호출은_Riot_API를_호출하지_않는다() throws Exception {
        // given: aggregating 플래그를 리플렉션으로 true로 세팅 (이미 집계 중인 상태 시뮬레이션)
        Field aggregatingField = MetaDeckServiceImpl.class.getDeclaredField("aggregating");
        aggregatingField.setAccessible(true);
        AtomicBoolean aggregating = (AtomicBoolean) aggregatingField.get(metaDeckService);
        aggregating.set(true);

        // when: 집계 중에 다시 호출
        metaDeckService.aggregateAndSave(LocalDate.of(2026, 6, 1));

        // then: Riot API 미호출 (집계 skip)
        verify(riotApiClient, never()).getChallenger();
        verify(riotApiClient, never()).getGrandmaster();
        verify(riotApiClient, never()).getMaster();

        // cleanup
        aggregating.set(false);
    }

    // ── aggregateAndSaveAsync (비동기 집계 중복·거부 처리) ────────────────

    @Test
    void 비동기_집계_중_중복_요청은_AGGREGATION_ALREADY_RUNNING_예외() throws Exception {
        // given: aggregating 플래그를 true로 세팅 (이미 집계 중인 상태)
        Field aggregatingField = MetaDeckServiceImpl.class.getDeclaredField("aggregating");
        aggregatingField.setAccessible(true);
        AtomicBoolean aggregating = (AtomicBoolean) aggregatingField.get(metaDeckService);
        aggregating.set(true);

        // when / then
        assertThatThrownBy(() -> metaDeckService.aggregateAndSaveAsync(LocalDate.of(2026, 6, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AGGREGATION_ALREADY_RUNNING);

        // cleanup
        aggregating.set(false);
    }

    @Test
    void executor_거부_시_AGGREGATION_QUEUE_FULL_예외_및_플래그_복원() throws Exception {
        // given: executor가 작업을 거부하도록 설정
        when(asyncAggregationRunner.run(any())).thenThrow(new RejectedExecutionException("큐 가득참"));

        // when / then
        assertThatThrownBy(() -> metaDeckService.aggregateAndSaveAsync(LocalDate.of(2026, 6, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AGGREGATION_QUEUE_FULL);

        // then: 플래그가 복원되어 다음 요청이 가능한 상태여야 한다
        Field aggregatingField = MetaDeckServiceImpl.class.getDeclaredField("aggregating");
        aggregatingField.setAccessible(true);
        AtomicBoolean aggregating = (AtomicBoolean) aggregatingField.get(metaDeckService);
        assertThat(aggregating.get()).isFalse();
    }

    @Test
    void executor_거부_후_재요청은_정상_등록된다() throws Exception {
        // given: 첫 번째 호출은 거부, 두 번째 호출은 성공
        when(asyncAggregationRunner.run(any()))
                .thenThrow(new RejectedExecutionException("큐 가득참"))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        LocalDate date = LocalDate.of(2026, 6, 1);

        // 첫 번째: 거부
        assertThatThrownBy(() -> metaDeckService.aggregateAndSaveAsync(date))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AGGREGATION_QUEUE_FULL);

        // 두 번째: 플래그가 복원됐으므로 정상 등록
        metaDeckService.aggregateAndSaveAsync(date);
        verify(asyncAggregationRunner, org.mockito.Mockito.times(2)).run(any());
    }

    // ── assignTierByRatio (패치 로직 핵심) ──────────────────────────────

    @Test
    void 패치버전_비교_최신_버전을_올바르게_선택한다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.EMERALD_PLUS))
                .thenReturn(List.of("16.9", "16.10", "16.11b", "16.11a"));

        // when
        Optional<String> latest = metaDeckService.findLatestPatchVersion(RankFilter.EMERALD_PLUS);

        // then: 알파벳 suffix까지 고려해 16.11b가 최신
        assertThat(latest).isPresent();
        assertThat(latest.get()).isEqualTo("16.11b");
    }

    @Test
    void UNKNOWN_패치버전은_최신_버전_선택에서_제외된다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.EMERALD_PLUS))
                .thenReturn(List.of("UNKNOWN", "16.10"));

        // when
        Optional<String> latest = metaDeckService.findLatestPatchVersion(RankFilter.EMERALD_PLUS);

        // then
        assertThat(latest).contains("16.10");
    }

    @Test
    void 집계_데이터가_전부_UNKNOWN이면_최신_버전은_empty다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.EMERALD_PLUS))
                .thenReturn(List.of("UNKNOWN"));

        // when
        Optional<String> latest = metaDeckService.findLatestPatchVersion(RankFilter.EMERALD_PLUS);

        // then
        assertThat(latest).isEmpty();
    }

    @Test
    void 매핑이_있으면_최신_패치는_매핑된_표시값_기준으로_계산된다() {
        // given: 원본은 16.13이지만 17.6으로 매핑되어 있어 표시값 기준(17.6)이 최신이어야 한다
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.EMERALD_PLUS))
                .thenReturn(List.of("16.13", "16.9"));
        when(clientVersionPatchMappingRepository.findAll()).thenReturn(List.of(
                com.tftgogo.domain.deck.entity.ClientVersionPatchMapping.builder()
                        .clientVersion("16.13").patchVersion("17.6").build()
        ));

        // when
        Optional<String> latest = metaDeckService.findLatestPatchVersion(RankFilter.EMERALD_PLUS);

        // then
        assertThat(latest).contains("17.6");
    }

    // ── normalizePatchVersion (#726) ─────────────────────────────────────
    // meta_decks에는 매핑 적용 전 원본 client version을 그대로 저장한다.
    // client version → 표시용 패치 번호 변환은 조회 시점(findLatestPatchVersion/resolveRawVersionsForPatch)에서만 이루어진다.

    @Test
    void game_version에서_client_version을_그대로_추출한다() {
        // when
        String patchVersion = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                metaDeckService, "normalizePatchVersion", "Version 16.13.702.1234");

        // then: 매핑 여부와 무관하게 원본 client version을 반환한다
        assertThat(patchVersion).isEqualTo("16.13");
        verify(clientVersionPatchMappingRepository, never()).findByClientVersion(anyString());
    }

    @Test
    void game_version이_비어있으면_UNKNOWN을_반환한다() {
        // when
        String patchVersion = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                metaDeckService, "normalizePatchVersion", (String) null);

        // then
        assertThat(patchVersion).isEqualTo("UNKNOWN");
    }

    // ── resolveRawVersionsForPatch ───────────────────────────────────────

    @Test
    void 표시_패치에_매핑된_원본_client_version들을_모두_반환한다() {
        // given
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.13", "16.14", "16.20"));
        when(clientVersionPatchMappingRepository.findAll()).thenReturn(List.of(
                com.tftgogo.domain.deck.entity.ClientVersionPatchMapping.builder()
                        .clientVersion("16.13").patchVersion("17.6").build(),
                com.tftgogo.domain.deck.entity.ClientVersionPatchMapping.builder()
                        .clientVersion("16.14").patchVersion("17.6").build()
        ));

        // when
        List<String> rawVersions = metaDeckService.resolveRawVersionsForPatch(RankFilter.MASTER_PLUS, "17.6");

        // then: 16.20은 매핑이 없어 원본값 그대로이므로 17.6 대상에서 제외된다
        assertThat(rawVersions).containsExactlyInAnyOrder("16.13", "16.14");
    }

    @Test
    void 매핑이_삭제되면_다음_조회부터_원본_client_version이_바로_노출된다() {
        // given: 매핑 테이블에 더 이상 16.13에 대한 항목이 없는 상태(삭제 직후)를 시뮬레이션
        when(metaDeckRepository.findDistinctPatchVersionsByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of("16.13"));
        when(clientVersionPatchMappingRepository.findAll()).thenReturn(List.of());

        // when
        Optional<String> latest = metaDeckService.findLatestPatchVersion(RankFilter.MASTER_PLUS);

        // then: 별도 소급 반영 없이 원본값이 즉시 표시값으로 사용된다
        assertThat(latest).contains("16.13");
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────

    private MetaDeck metaDeck(String signature, String patchVersion, String tier, double playRate) {
        return MetaDeck.builder()
                .signature(signature)
                .rankFilter(RankFilter.MASTER_PLUS)
                .name("테스트 덱")
                .patchVersion(patchVersion)
                .tier(tier)
                .playRate(playRate)
                .winRate(50.0)
                .top4Rate(60.0)
                .avgPlacement(4.0)
                .sampleSize(100)
                .dataStartDate(LocalDate.of(2026, 6, 1))
                .build();
    }
}
