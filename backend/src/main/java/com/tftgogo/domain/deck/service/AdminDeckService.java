package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.request.DeckCurationRequest;
import com.tftgogo.domain.deck.dto.response.AdminDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

import java.util.List;

public interface AdminDeckService {

    /** 최신 패치 기준 관리자 덱 목록 조회 */
    List<AdminDeckResponse> getAdminDecks(RankFilter rankFilter);

    /** 특정 덱 큐레이션 저장/수정 */
    AdminDeckResponse updateCuration(Long deckId, DeckCurationRequest request);

    /** 특정 덱 큐레이션 초기화 */
    void resetCuration(Long deckId);
}
