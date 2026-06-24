package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.request.HeroAugmentDeckRequest;
import com.tftgogo.domain.deck.dto.response.HeroAugmentDeckResponse;
import com.tftgogo.domain.deck.entity.HeroAugmentDeck;
import com.tftgogo.domain.deck.repository.HeroAugmentDeckRepository;
import com.tftgogo.domain.deck.service.HeroAugmentDeckService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroAugmentDeckServiceImpl implements HeroAugmentDeckService {

    private final HeroAugmentDeckRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<HeroAugmentDeckResponse> findAll() {
        return repository.findAll().stream()
                .map(HeroAugmentDeckResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public HeroAugmentDeckResponse create(HeroAugmentDeckRequest request) {
        HeroAugmentDeck deck = HeroAugmentDeck.builder()
                .name(request.getName())
                .description(request.getDescription())
                .champions(request.getChampions())
                .traits(request.getTraits())
                .boardPositions(request.getBoardPositions())
                .heroAugments(request.getHeroAugments())
                .recommended(request.isRecommended())
                .sortOrder(request.getSortOrder())
                .grade(request.getGrade())
                .build();
        return HeroAugmentDeckResponse.from(repository.save(deck));
    }

    @Override
    @Transactional
    public HeroAugmentDeckResponse update(Long id, HeroAugmentDeckRequest request) {
        HeroAugmentDeck deck = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HERO_AUGMENT_DECK_NOT_FOUND));
        deck.update(request.getName(), request.getDescription(), request.getChampions(),
                request.getTraits(), request.getBoardPositions(), request.getHeroAugments(),
                request.isRecommended(), request.getSortOrder(), request.getGrade());
        return HeroAugmentDeckResponse.from(deck);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HeroAugmentDeck deck = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HERO_AUGMENT_DECK_NOT_FOUND));
        repository.delete(deck);
    }
}
