package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<HeroAugmentDeckResponse> findAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(HeroAugmentDeckResponse::from)
                .toList();
    }

    private void validateJsonFields(HeroAugmentDeckRequest request) {
        validateJsonArray(request.getHeroAugments(), "heroAugments");
        validateJsonArray(request.getChampions(), "champions");
        validateJsonArray(request.getTraits(), "traits");
        validateJsonObject(request.getBoardPositions(), "boardPositions");
    }

    private void validateJsonArray(String value, String fieldName) {
        if (value == null) return;
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isArray()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        fieldName + " 필드가 유효한 JSON 배열이 아닙니다.");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    fieldName + " 필드가 유효한 JSON 배열이 아닙니다.");
        }
    }

    private void validateJsonObject(String value, String fieldName) {
        if (value == null) return;
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isObject()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        fieldName + " 필드가 유효한 JSON 객체가 아닙니다.");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    fieldName + " 필드가 유효한 JSON 객체가 아닙니다.");
        }
    }

    @Override
    @Transactional
    public HeroAugmentDeckResponse create(HeroAugmentDeckRequest request) {
        validateJsonFields(request);
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
        validateJsonFields(request);
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
