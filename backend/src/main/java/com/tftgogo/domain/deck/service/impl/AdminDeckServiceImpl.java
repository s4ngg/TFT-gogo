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
import com.tftgogo.global.config.CacheConfig;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(value = CacheConfig.META_DECKS, allEntries = true)
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
    @CacheEvict(value = CacheConfig.META_DECKS, allEntries = true)
    public void resetCuration(Long deckId) {
        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deckCurationRepository
                .findBySignatureAndRankFilter(deck.getSignature(), deck.getRankFilter())
                .ifPresent(deckCurationRepository::delete);
    }

    /** boardPositions / playGuide / heroAugments JSON 문자열 유효성 검증 */
    private void validateJsonFields(DeckCurationRequest request) {
        validateBoardPositions(request.getBoardPositions());
        validateJson(request.getPlayGuide());
        validateJson(request.getHeroAugments());
    }

    /**
     * boardPositions 구조 검증
     * 기대 구조: {"5": {"TFT17_Ahri": {"row":0-3, "col":0-6, "items":[...]}}, ...}
     * 레벨 키는 5~9, row는 0~3, col은 0~6 범위여야 한다.
     */
    @SuppressWarnings("unchecked")
    private void validateBoardPositions(String json) {
        if (json == null) return;
        try {
            Map<String, Object> levelMap = objectMapper.readValue(json, Map.class);
            for (Map.Entry<String, Object> levelEntry : levelMap.entrySet()) {
                int level;
                try {
                    level = Integer.parseInt(levelEntry.getKey());
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }
                if (level < 5 || level > 9) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }
                if (!(levelEntry.getValue() instanceof Map)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }
                Map<String, Object> champMap = (Map<String, Object>) levelEntry.getValue();
                for (Map.Entry<String, Object> champEntry : champMap.entrySet()) {
                    if (!(champEntry.getValue() instanceof Map)) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT);
                    }
                    Map<String, Object> pos = (Map<String, Object>) champEntry.getValue();
                    int row = toInt(pos.get("row"));
                    int col = toInt(pos.get("col"));
                    if (row < 0 || row > 3 || col < 0 || col > 6) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT);
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private int toInt(Object value) {
        if (value == null) throw new BusinessException(ErrorCode.INVALID_INPUT);
        if (value instanceof Integer i) return i;
        // Long은 허용하되 소수(Double/Float)는 좌표로 유효하지 않으므로 거부
        if (value instanceof Long l) return Math.toIntExact(l);
        throw new BusinessException(ErrorCode.INVALID_INPUT);
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
