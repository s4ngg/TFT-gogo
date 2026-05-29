package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.MetaDeckControllerDocs;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class MetaDeckController implements MetaDeckControllerDocs {

    private final MetaDeckService metaDeckService;

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<MetaDeckListResponse>> getMetaDecks(
            @RequestParam(defaultValue = "EMERALD_PLUS") RankFilter rankFilter) {
        MetaDeckListResponse response = metaDeckService.getMetaDecks(rankFilter);
        return ResponseEntity.ok(ApiResponse.success("메타 덱 조회 성공", response));
    }

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
}
