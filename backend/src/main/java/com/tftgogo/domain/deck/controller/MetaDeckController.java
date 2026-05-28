package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.MetaDeckControllerDocs;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class MetaDeckController implements MetaDeckControllerDocs {

    private final MetaDeckService metaDeckService;

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<List<MetaDeckResponse>>> getMetaDecks(
            @RequestParam(defaultValue = "EMERALD_PLUS") RankFilter rankFilter) {
        List<MetaDeckResponse> decks = metaDeckService.getMetaDecks(rankFilter);
        return ResponseEntity.ok(ApiResponse.success("메타 덱 조회 성공", decks));
    }

    @PostMapping("/meta/aggregate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerAggregate() {
        metaDeckService.aggregateAndSave();
        return ResponseEntity.ok(ApiResponse.success("집계가 완료되었습니다."));
    }
}
