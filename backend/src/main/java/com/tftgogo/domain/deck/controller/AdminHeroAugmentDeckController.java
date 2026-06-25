package com.tftgogo.domain.deck.controller;

import com.tftgogo.domain.deck.controller.docs.AdminHeroAugmentDeckControllerDocs;
import com.tftgogo.domain.deck.dto.request.HeroAugmentDeckRequest;
import com.tftgogo.domain.deck.dto.response.HeroAugmentDeckResponse;
import com.tftgogo.domain.deck.service.HeroAugmentDeckService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hero-augment-decks")
@RequiredArgsConstructor
public class AdminHeroAugmentDeckController implements AdminHeroAugmentDeckControllerDocs {

    private final HeroAugmentDeckService heroAugmentDeckService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HeroAugmentDeckResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("영웅증강 덱 목록 조회 성공", heroAugmentDeckService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HeroAugmentDeckResponse>> create(
            @RequestBody @Valid HeroAugmentDeckRequest request) {
        HeroAugmentDeckResponse response = heroAugmentDeckService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("영웅증강 덱 생성 성공", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HeroAugmentDeckResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid HeroAugmentDeckRequest request) {
        return ResponseEntity.ok(ApiResponse.success("영웅증강 덱 수정 성공", heroAugmentDeckService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        heroAugmentDeckService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("영웅증강 덱 삭제 성공"));
    }
}
