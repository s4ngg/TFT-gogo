package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.dto.request.DeckCurationRequest;
import com.tftgogo.domain.deck.dto.response.AdminDeckResponse;
import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.DeckCurationRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeckServiceImplTest {

    @Mock
    private MetaDeckRepository metaDeckRepository;

    @Mock
    private DeckCurationRepository deckCurationRepository;

    @Mock
    private MetaDeckService metaDeckService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminDeckServiceImpl adminDeckService;

    // ── getAdminDecks ────────────────────────────────────────────────────

    @Test
    void 최신_패치_없으면_빈_리스트를_반환한다() {
        // given
        when(metaDeckService.findLatestPatchVersion(RankFilter.MASTER_PLUS))
                .thenReturn(Optional.empty());

        // when
        List<AdminDeckResponse> result = adminDeckService.getAdminDecks(RankFilter.MASTER_PLUS);

        // then
        assertThat(result).isEmpty();
        verify(metaDeckRepository, never()).findByRankFilterAndPatchVersion(any(), any());
    }

    @Test
    void 큐레이션_없는_덱은_null_큐레이션으로_응답한다() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        when(metaDeckService.findLatestPatchVersion(RankFilter.MASTER_PLUS))
                .thenReturn(Optional.of("16.11"));
        when(metaDeckRepository.findByRankFilterAndPatchVersion(RankFilter.MASTER_PLUS, "16.11"))
                .thenReturn(List.of(deck));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of());

        // when
        List<AdminDeckResponse> result = adminDeckService.getAdminDecks(RankFilter.MASTER_PLUS);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomName()).isNull();
        assertThat(result.get(0).isHidden()).isFalse();
    }

    @Test
    void 큐레이션_있는_덱은_큐레이션_데이터를_포함한다() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        DeckCuration curation = DeckCuration.builder()
                .signature("sig1")
                .rankFilter(RankFilter.MASTER_PLUS)
                .customName("커스텀 덱 이름")
                .hidden(false)
                .sortPriority(1)
                .build();

        when(metaDeckService.findLatestPatchVersion(RankFilter.MASTER_PLUS))
                .thenReturn(Optional.of("16.11"));
        when(metaDeckRepository.findByRankFilterAndPatchVersion(RankFilter.MASTER_PLUS, "16.11"))
                .thenReturn(List.of(deck));
        when(deckCurationRepository.findByRankFilter(RankFilter.MASTER_PLUS))
                .thenReturn(List.of(curation));

        // when
        List<AdminDeckResponse> result = adminDeckService.getAdminDecks(RankFilter.MASTER_PLUS);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomName()).isEqualTo("커스텀 덱 이름");
        assertThat(result.get(0).getSortPriority()).isEqualTo(1);
    }

    // ── updateCuration ───────────────────────────────────────────────────

    @Test
    void 덱이_없으면_DECK_NOT_FOUND_예외() {
        // given
        when(metaDeckRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(99L, request(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DECK_NOT_FOUND);
    }

    @Test
    void 큐레이션_없으면_새로_생성된다() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        when(metaDeckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckCurationRepository.findBySignatureAndRankFilter("sig1", RankFilter.MASTER_PLUS))
                .thenReturn(Optional.empty());
        when(deckCurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        adminDeckService.updateCuration(1L, request("새 덱 이름"));

        // then
        ArgumentCaptor<DeckCuration> captor = ArgumentCaptor.forClass(DeckCuration.class);
        verify(deckCurationRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomName()).isEqualTo("새 덱 이름");
        assertThat(captor.getValue().getSignature()).isEqualTo("sig1");
    }

    @Test
    void 기존_큐레이션이_있으면_update가_호출된다() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        DeckCuration existing = spy(DeckCuration.builder()
                .signature("sig1")
                .rankFilter(RankFilter.MASTER_PLUS)
                .customName("기존 이름")
                .hidden(false)
                .build());

        when(metaDeckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckCurationRepository.findBySignatureAndRankFilter("sig1", RankFilter.MASTER_PLUS))
                .thenReturn(Optional.of(existing));
        when(deckCurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        adminDeckService.updateCuration(1L, request("업데이트 이름"));

        // then
        verify(existing).update(eq("업데이트 이름"), anyBoolean(), any(), any(), any(), any(), any());
    }

    @Test
    void 유효하지_않은_boardPositions_레벨_키_INVALID_INPUT() {
        // given (validateJsonFields는 findById 이전에 실행)
        DeckCurationRequest req = requestWithBoardPositions("{\"4\": {}}");  // 레벨 4 = 범위 밖

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 유효하지_않은_boardPositions_row_범위_INVALID_INPUT() {
        // given
        String json = "{\"5\": {\"TFT17_Ahri\": {\"row\": 4, \"col\": 3}}}";  // row 4 = 범위 밖
        DeckCurationRequest req = requestWithBoardPositions(json);

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 유효하지_않은_boardPositions_col_범위_INVALID_INPUT() {
        // given
        String json = "{\"9\": {\"TFT17_Ahri\": {\"row\": 3, \"col\": 7}}}";  // col 7 = 범위 밖
        DeckCurationRequest req = requestWithBoardPositions(json);

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 경계값_boardPositions_레벨5_row3_col6_정상처리() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        when(metaDeckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckCurationRepository.findBySignatureAndRankFilter(any(), any())).thenReturn(Optional.empty());
        when(deckCurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = "{\"5\": {\"TFT17_Ahri\": {\"row\": 3, \"col\": 6}}}";
        DeckCurationRequest req = requestWithBoardPositions(json);

        // when / then (예외 없이 정상 실행)
        adminDeckService.updateCuration(1L, req);
    }

    @Test
    void boardPositions_JSON_파싱_실패시_INVALID_INPUT() {
        // given
        DeckCurationRequest req = requestWithBoardPositions("not-valid-json");

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void playGuide_JSON_파싱_실패시_INVALID_INPUT() {
        // given
        DeckCurationRequest req = requestWithPlayGuide("invalid-json{");

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void heroAugments_JSON_파싱_실패시_INVALID_INPUT() {
        // given
        DeckCurationRequest req = requestWithHeroAugments("[{invalid}]");

        // when / then
        assertThatThrownBy(() -> adminDeckService.updateCuration(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    // ── resetCuration ────────────────────────────────────────────────────

    @Test
    void 덱_없으면_reset시_DECK_NOT_FOUND_예외() {
        // given
        when(metaDeckRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> adminDeckService.resetCuration(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DECK_NOT_FOUND);
    }

    @Test
    void 큐레이션_있을_때_delete가_호출된다() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");
        DeckCuration curation = DeckCuration.builder()
                .signature("sig1")
                .rankFilter(RankFilter.MASTER_PLUS)
                .build();

        when(metaDeckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckCurationRepository.findBySignatureAndRankFilter("sig1", RankFilter.MASTER_PLUS))
                .thenReturn(Optional.of(curation));

        // when
        adminDeckService.resetCuration(1L);

        // then
        verify(deckCurationRepository).delete(curation);
    }

    @Test
    void 큐레이션_없을_때_delete_미호출_예외_없음() {
        // given
        MetaDeck deck = metaDeck(1L, "sig1", RankFilter.MASTER_PLUS, "16.11");

        when(metaDeckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckCurationRepository.findBySignatureAndRankFilter("sig1", RankFilter.MASTER_PLUS))
                .thenReturn(Optional.empty());

        // when
        adminDeckService.resetCuration(1L);

        // then
        verify(deckCurationRepository, never()).delete(any());
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private MetaDeck metaDeck(Long id, String signature, RankFilter rankFilter, String patchVersion) {
        MetaDeck deck = MetaDeck.builder()
                .signature(signature)
                .rankFilter(rankFilter)
                .name("테스트 덱")
                .patchVersion(patchVersion)
                .tier("S")
                .playRate(5.0)
                .winRate(55.0)
                .top4Rate(65.0)
                .avgPlacement(3.5)
                .sampleSize(100)
                .dataStartDate(LocalDate.of(2026, 6, 1))
                .build();
        try {
            Field idField = MetaDeck.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(deck, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return deck;
    }

    private DeckCurationRequest request(String customName) {
        return requestBuilder(customName, null, null, null);
    }

    private DeckCurationRequest requestWithBoardPositions(String boardPositions) {
        return requestBuilder(null, boardPositions, null, null);
    }

    private DeckCurationRequest requestWithPlayGuide(String playGuide) {
        return requestBuilder(null, null, playGuide, null);
    }

    private DeckCurationRequest requestWithHeroAugments(String heroAugments) {
        return requestBuilder(null, null, null, heroAugments);
    }

    private DeckCurationRequest requestBuilder(String customName, String boardPositions,
                                                String playGuide, String heroAugments) {
        try {
            DeckCurationRequest req = new DeckCurationRequest();
            setField(req, "customName", customName);
            setField(req, "boardPositions", boardPositions);
            setField(req, "playGuide", playGuide);
            setField(req, "heroAugments", heroAugments);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
