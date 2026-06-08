package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.domain.ai.service.AiRecommendService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendController {

    private final AiRecommendService aiRecommendService;

    /**
     * 소환사 전적 기반 AI 덱 추천.
     *
     * 프론트 → Spring → AI 서버 흐름의 Spring 프록시 엔드포인트.
     * AI 서버 오류 시 null을 반환하며 프론트는 목데이터로 fallback한다.
     */
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<AiRecommendResponse>> recommend(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        AiRecommendResponse result = aiRecommendService.recommend(gameName, tagLine);

        if (result == null) {
            return ResponseEntity.ok(
                    ApiResponse.success("AI 서버 연결 실패 — 목데이터를 사용하세요.", null)
            );
        }

        return ResponseEntity.ok(ApiResponse.success("AI 추천 완료", result));
    }
}
