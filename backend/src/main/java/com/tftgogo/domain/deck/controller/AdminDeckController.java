package com.tftgogo.domain.deck.controller;

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
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final MetaDeckService metaDeckService;
    private final ObjectMapper objectMapper;

    /** 전체 메타 덱 목록 (큐레이션 포함) — #133: service의 comparePatchVersions 기반 최신 패치 사용 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminDeckResponse>>> listDecks(
            @RequestParam(defaultValue = "MASTER_PLUS") RankFilter rankFilter) {

        String latestPatch = metaDeckService.findLatestPatchVersion(rankFilter).orElse(null);
        if (latestPatch == null) {
            return ResponseEntity.ok(ApiResponse.success("관리자 덱 목록 조회 성공", List.of()));
        }
        List<MetaDeck> decks = metaDeckRepository.findByRankFilterAndPatchVersion(rankFilter, latestPatch);

        Map<String, DeckCuration> curationMap = deckCurationRepository
                .findByRankFilter(rankFilter).stream()
                .collect(Collectors.toMap(DeckCuration::getSignature, Function.identity()));

        List<AdminDeckResponse> responses = decks.stream()
                .map(deck -> AdminDeckResponse.from(deck, curationMap.get(deck.getSignature())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("관리자 덱 목록 조회 성공", responses));
    }

    /** 수동 집계 트리거 — #129: admin 경로로 이동하여 인증 강제 */
    @PostMapping("/meta/aggregate")
    public ResponseEntity<ApiResponse<Void>> triggerAggregate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            metaDeckService.aggregateAndSave();
        } else {
            metaDeckService.aggregateAndSave(date);
        }
        return ResponseEntity.ok(ApiResponse.success("집계가 완료되었습니다."));
    }

    /** 특정 덱 큐레이션 저장/수정 — #135: BusinessException, #136: JSON 검증 */
    @PatchMapping("/{deckId}")
    public ResponseEntity<ApiResponse<AdminDeckResponse>> updateCuration(
            @PathVariable Long deckId,
            @RequestBody DeckCurationRequest request) {

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
                    .build();
        } else {
            curation.update(request.getCustomName(), request.isHidden(),
                    request.getSortPriority(), request.getCuratorNote(),
                    request.getBoardPositions(), request.getPlayGuide());
        }

        deckCurationRepository.save(curation);
        return ResponseEntity.ok(ApiResponse.success("큐레이션 저장 완료", AdminDeckResponse.from(deck, curation)));
    }

    /** 큐레이션 초기화 — #135: BusinessException */
    @DeleteMapping("/{deckId}/curation")
    public ResponseEntity<ApiResponse<Void>> resetCuration(@PathVariable Long deckId) {
        MetaDeck deck = metaDeckRepository.findById(deckId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deckCurationRepository
                .findBySignatureAndRankFilter(deck.getSignature(), deck.getRankFilter())
                .ifPresent(deckCurationRepository::delete);

        return ResponseEntity.ok(ApiResponse.success("큐레이션 초기화 완료", null));
    }

    /** #136: boardPositions, playGuide JSON 문자열 parse 검증 */
    private void validateJsonFields(DeckCurationRequest request) {
        if (request.getBoardPositions() != null) {
            try {
                objectMapper.readTree(request.getBoardPositions());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }
        if (request.getPlayGuide() != null) {
            try {
                objectMapper.readTree(request.getPlayGuide());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }
    }
}
