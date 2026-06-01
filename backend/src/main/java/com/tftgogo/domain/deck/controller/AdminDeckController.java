package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.dto.request.DeckCurationRequest;
import com.tftgogo.domain.deck.dto.response.AdminDeckResponse;
import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.DeckCurationRepository;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/decks")
@RequiredArgsConstructor
public class AdminDeckController {

    private final MetaDeckRepository metaDeckRepository;
    private final DeckCurationRepository deckCurationRepository;

    /** 전체 메타 덱 목록 (큐레이션 포함) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminDeckResponse>>> listDecks(
            @RequestParam(defaultValue = "MASTER_PLUS") RankFilter rankFilter) {

        String latestPatch = metaDeckRepository.findLatestPatchVersion(rankFilter).orElse("");
        List<MetaDeck> decks = metaDeckRepository.findByRankFilterAndPatchVersion(rankFilter, latestPatch);

        Map<String, DeckCuration> curationMap = deckCurationRepository
                .findByRankFilter(rankFilter).stream()
                .collect(Collectors.toMap(DeckCuration::getSignature, Function.identity()));

        List<AdminDeckResponse> responses = decks.stream()
                .map(deck -> AdminDeckResponse.from(deck, curationMap.get(deck.getSignature())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("관리자 덱 목록 조회 성공", responses));
    }

    /** 특정 덱 큐레이션 저장/수정 */
    @PatchMapping("/{deckId}")
    public ResponseEntity<ApiResponse<AdminDeckResponse>> updateCuration(
            @PathVariable Long deckId,
            @RequestBody DeckCurationRequest request) {

        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("덱을 찾을 수 없습니다: " + deckId));

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
                    .build();
        } else {
            curation.update(request.getCustomName(), request.isHidden(),
                    request.getSortPriority(), request.getCuratorNote());
        }

        deckCurationRepository.save(curation);
        return ResponseEntity.ok(ApiResponse.success("큐레이션 저장 완료", AdminDeckResponse.from(deck, curation)));
    }

    /** 큐레이션 초기화 (자동 이름으로 되돌리기) */
    @DeleteMapping("/{deckId}/curation")
    public ResponseEntity<ApiResponse<Void>> resetCuration(@PathVariable Long deckId) {
        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("덱을 찾을 수 없습니다: " + deckId));

        deckCurationRepository
                .findBySignatureAndRankFilter(deck.getSignature(), deck.getRankFilter())
                .ifPresent(deckCurationRepository::delete);

        return ResponseEntity.ok(ApiResponse.success("큐레이션 초기화 완료", null));
    }
}
