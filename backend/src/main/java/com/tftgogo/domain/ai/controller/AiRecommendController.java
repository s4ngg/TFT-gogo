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
     * 소환사 전적 기반 AI 덱 추천 (로그인 필수).
     *
     * - 전적 부족: 200 success:true data:null
     * - 소환사/AI 서버 오류: GlobalExceptionHandler가 적절한 HTTP 상태로 응답
     */
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<AiRecommendResponse>> recommend(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        AiRecommendResponse result = aiRecommendService.recommend(gameName, tagLine);
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.success("전적 데이터가 부족합니다.", null));
        }
        return ResponseEntity.ok(ApiResponse.success("AI 추천 완료", result));
    }
}
