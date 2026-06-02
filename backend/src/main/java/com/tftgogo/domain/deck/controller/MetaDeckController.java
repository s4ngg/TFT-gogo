package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.MetaDeckControllerDocs;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
