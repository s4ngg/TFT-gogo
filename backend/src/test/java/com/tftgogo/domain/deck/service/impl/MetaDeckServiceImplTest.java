package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(metaDeckRepository.findMetaDecksByPickRate(eq(RankFilter.MASTER_PLUS), eq("16.11"), anyDouble()))
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
        when(metaDeckRepository.findMetaDecksByPickRate(any(), anyString(), anyDouble()))
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
        when(metaDeckRepository.findMetaDecksByPickRate(any(), anyString(), anyDouble()))
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
