package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.dto.request.DeckCurationRequest;
import com.tftgogo.domain.deck.dto.response.AdminDeckResponse;
import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.DeckCurationRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.AdminDeckService;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDeckServiceImpl implements AdminDeckService {

    private final MetaDeckRepository metaDeckRepository;
    private final DeckCurationRepository deckCurationRepository;
    private final MetaDeckService metaDeckService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminDeckResponse> getAdminDecks(RankFilter rankFilter) {
        return metaDeckService.findLatestPatchVersion(rankFilter)
                .map(patch -> buildAdminResponses(rankFilter, patch))
                .orElse(List.of());
    }

    private List<AdminDeckResponse> buildAdminResponses(RankFilter rankFilter, String patch) {
        List<MetaDeck> decks = metaDeckRepository.findByRankFilterAndPatchVersion(rankFilter, patch);
        Map<String, DeckCuration> curationMap = deckCurationRepository
                .findByRankFilter(rankFilter).stream()
                .collect(Collectors.toMap(DeckCuration::getSignature, Function.identity()));
        return decks.stream()
                .map(deck -> AdminDeckResponse.from(deck, curationMap.get(deck.getSignature())))
                .toList();
    }

    @Override
    @Transactional
    public AdminDeckResponse updateCuration(Long deckId, DeckCurationRequest request) {
        validateJsonFields(request);

        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        DeckCuration curation = deckCurationRepository
                .findBySignatureAndRankFilter(deck.getSignature(), deck.getRankFilter())
                .orElse(null);

        if (curation == null) {
            curation = DeckCuration.builder()
                    .signature(deck.getSignature())
                    .rankFilter(deck.getRankFilter())
                    .customName(request.getCustomName())
                    .hidden(request.isHidden())
                    .sortPriority(request.getSortPriority())
                    .curatorNote(request.getCuratorNote())
                    .boardPositions(request.getBoardPositions())
                    .playGuide(request.getPlayGuide())
                    .heroAugments(request.getHeroAugments())
                    .build();
        } else {
            curation.update(request.getCustomName(), request.isHidden(),
                    request.getSortPriority(), request.getCuratorNote(),
                    request.getBoardPositions(), request.getPlayGuide(),
                    request.getHeroAugments());
        }

        deckCurationRepository.save(curation);
        return AdminDeckResponse.from(deck, curation);
    }

    @Override
    @Transactional
    public void resetCuration(Long deckId) {
        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deckCurationRepository
                .findBySignatureAndRankFilter(deck.getSignature(), deck.getRankFilter())
                .ifPresent(deckCurationRepository::delete);
    }

    /** boardPositions / playGuide / heroAugments JSON 문자열 유효성 검증 */
    private void validateJsonFields(DeckCurationRequest request) {
        validateJson(request.getBoardPositions());
        validateJson(request.getPlayGuide());
        validateJson(request.getHeroAugments());
    }

    private void validateJson(String json) {
        if (json == null) return;
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
