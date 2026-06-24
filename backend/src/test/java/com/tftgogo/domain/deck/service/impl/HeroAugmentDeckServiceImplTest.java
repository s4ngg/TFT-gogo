package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.dto.request.HeroAugmentDeckRequest;
import com.tftgogo.domain.deck.dto.response.HeroAugmentDeckResponse;
import com.tftgogo.domain.deck.entity.HeroAugmentDeck;
import com.tftgogo.domain.deck.repository.HeroAugmentDeckRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroAugmentDeckServiceImplTest {

    @Mock
    private HeroAugmentDeckRepository repository;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private HeroAugmentDeckServiceImpl service;

    // ── findAll ──────────────────────────────────────────────────────────

    @Test
    void 목록_조회는_sortOrder_오름차순으로_반환한다() {
        HeroAugmentDeck deck = HeroAugmentDeck.builder()
                .name("테스트 덱").recommended(true).sortOrder(1).build();
        when(repository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(deck));

        List<HeroAugmentDeckResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 덱");
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    void 정상_요청으로_덱을_생성한다() throws Exception {
        HeroAugmentDeckRequest request = buildRequest("덱1",
                "[{\"championId\":\"tft17_nasus\"}]",
                "[{\"name\":\"신성\"}]",
                "{\"row0\":\"tft17_nasus\"}",
                "[{\"augmentName\":\"꽁 나서스\"}]");
        HeroAugmentDeck saved = HeroAugmentDeck.builder()
                .name("덱1").recommended(true).sortOrder(0).build();
        when(repository.save(any())).thenReturn(saved);

        HeroAugmentDeckResponse response = service.create(request);

        assertThat(response.getName()).isEqualTo("덱1");
        verify(repository).save(any());
    }

    @Test
    void 잘못된_heroAugments_JSON이면_400_예외를_던진다() throws Exception {
        HeroAugmentDeckRequest request = buildRequest("덱1", null, null, null, "invalid json");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void heroAugments가_배열이_아니면_400_예외를_던진다() throws Exception {
        HeroAugmentDeckRequest request = buildRequest("덱1", null, null, null, "{\"key\":\"val\"}");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void boardPositions가_객체가_아니면_400_예외를_던진다() throws Exception {
        HeroAugmentDeckRequest request = buildRequest("덱1", null, null, "[1,2,3]", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_ID_수정_시_NOT_FOUND_예외를_던진다() throws Exception {
        HeroAugmentDeckRequest request = buildRequest("덱1", null, null, null, null);
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.HERO_AUGMENT_DECK_NOT_FOUND));
    }

    // ── delete ───────────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_ID_삭제_시_NOT_FOUND_예외를_던진다() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.HERO_AUGMENT_DECK_NOT_FOUND));
    }

    @Test
    void 정상_ID_삭제_시_repository_delete를_호출한다() {
        HeroAugmentDeck deck = HeroAugmentDeck.builder()
                .name("삭제할 덱").recommended(true).sortOrder(0).build();
        when(repository.findById(1L)).thenReturn(Optional.of(deck));

        service.delete(1L);

        verify(repository).delete(deck);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private HeroAugmentDeckRequest buildRequest(String name, String champions, String traits,
                                                String boardPositions, String heroAugments)
            throws Exception {
        HeroAugmentDeckRequest req = new HeroAugmentDeckRequest();
        setField(req, "name", name);
        setField(req, "champions", champions);
        setField(req, "traits", traits);
        setField(req, "boardPositions", boardPositions);
        setField(req, "heroAugments", heroAugments);
        setField(req, "recommended", true);
        setField(req, "sortOrder", 0);
        return req;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
