package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.dto.request.DeckCurationRequest;
import com.tftgogo.domain.deck.dto.response.AdminDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.AdminDeckService;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/decks")
@RequiredArgsConstructor
public class AdminDeckController {

    private final AdminDeckService adminDeckService;
    private final MetaDeckService metaDeckService;

    /** 전체 메타 덱 목록 (큐레이션 포함) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminDeckResponse>>> listDecks(
            @RequestParam(name = "rankFilter", defaultValue = "MASTER_PLUS") RankFilter rankFilter) {

        List<AdminDeckResponse> responses = adminDeckService.getAdminDecks(rankFilter);
        return ResponseEntity.ok(ApiResponse.success("관리자 덱 목록 조회 성공", responses));
    }

    /** 수동 집계 트리거 — #129: admin 경로, #130: 비동기 202 Accepted */
    @PostMapping("/meta/aggregate")
    public ResponseEntity<ApiResponse<Void>> triggerAggregate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null)
                ? date
                : LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1);
        metaDeckService.aggregateAndSaveAsync(targetDate);
        return ResponseEntity.accepted().body(ApiResponse.success("집계가 시작되었습니다."));
    }

    /** 특정 덱 큐레이션 저장/수정 — #135: BusinessException, #136: JSON 검증 */
    @PatchMapping("/{deckId}")
    public ResponseEntity<ApiResponse<AdminDeckResponse>> updateCuration(
            @PathVariable Long deckId,
            @RequestBody @Valid DeckCurationRequest request) {

        AdminDeckResponse response = adminDeckService.updateCuration(deckId, request);
        return ResponseEntity.ok(ApiResponse.success("큐레이션 저장 완료", response));
    }

    /** 큐레이션 초기화 — #135: BusinessException */
    @DeleteMapping("/{deckId}/curation")
    public ResponseEntity<ApiResponse<Void>> resetCuration(@PathVariable Long deckId) {
        adminDeckService.resetCuration(deckId);
        return ResponseEntity.ok(ApiResponse.success("큐레이션 초기화 완료", null));
    }
}
