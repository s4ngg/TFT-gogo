package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.MetaDeckControllerDocs;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class MetaDeckController implements MetaDeckControllerDocs {

    private final MetaDeckService metaDeckService;

    // 메타 덱 목록 조회
    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<List<MetaDeckResponse>>> getMetaDecks() {
        List<MetaDeckResponse> decks = metaDeckService.getMetaDecks();
        return ResponseEntity.ok(ApiResponse.success("메타 덱 조회 성공", decks));
    }

    // 수동 집계 트리거 (관리자용 - 추후 권한 제한)
    @PostMapping("/meta/aggregate")
    public ResponseEntity<ApiResponse<Void>> triggerAggregate() {
        metaDeckService.aggregateAndSave();
        return ResponseEntity.ok(ApiResponse.success("집계가 완료되었습니다."));
    }
}
