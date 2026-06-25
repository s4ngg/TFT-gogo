package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.request.HeroAugmentDeckRequest;
import com.tftgogo.domain.deck.dto.response.HeroAugmentDeckResponse;

import java.util.List;

public interface HeroAugmentDeckService {

    List<HeroAugmentDeckResponse> findAll();

    HeroAugmentDeckResponse create(HeroAugmentDeckRequest request);

    HeroAugmentDeckResponse update(Long id, HeroAugmentDeckRequest request);

    void delete(Long id);
}
