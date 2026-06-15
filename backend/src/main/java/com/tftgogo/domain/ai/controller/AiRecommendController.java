package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.domain.ai.service.AiRecommendService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendController {

    private static final Logger logger = LogManager.getLogger(AiRecommendController.class);

    private final AiRecommendService aiRecommendService;

    /**
     * 소환사 전적 기반 AI 덱 추천 (로그인 필수).
     *
     * - 전적 부족: 200 success:true data:null (프론트가 빈 상태로 표시)
     * - AI 서버 장애: 502 success:false (프론트가 에러로 표시)
     */
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<AiRecommendResponse>> recommend(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        try {
            AiRecommendResponse result = aiRecommendService.recommend(gameName, tagLine);
            if (result == null) {
                return ResponseEntity.ok(ApiResponse.success("전적 데이터가 부족합니다.", null));
            }
            return ResponseEntity.ok(ApiResponse.success("AI 추천 완료", result));
        } catch (Exception e) {
            logger.error("AI 추천 처리 중 오류 발생: gameName={}, tagLine={}", gameName, tagLine, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.fail("AI 서버 연결 실패"));
        }
    }
}
