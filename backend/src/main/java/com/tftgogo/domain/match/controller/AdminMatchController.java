package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.AdminMatchControllerDocs;
import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.global.response.ApiResponse;
import com.tftgogo.global.riot.RiotRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/match")
@RequiredArgsConstructor
public class AdminMatchController implements AdminMatchControllerDocs {

    private static final DateTimeFormatter KST_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CachedMatchRepository cachedMatchRepository;
    private final RiotRateLimiter riotRateLimiter;

    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats() {
        CacheStatsResponse stats = CacheStatsResponse.of(
                cachedMatchRepository.count(),
                cachedMatchRepository.countByQueueId(1100),
                cachedMatchRepository.countByQueueId(1090),
                cachedMatchRepository.findMaxGameDatetime().orElse(null),
                cachedMatchRepository.findMinGameDatetime().orElse(null),
                cachedMatchRepository.findLatestCachedAt()
                        .map(dt -> dt.atZone(ZoneId.of("Asia/Seoul")).format(KST_FMT))
                        .orElse(null)
        );
        return ResponseEntity.ok(ApiResponse.success("캐시 통계 조회 성공", stats));
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<ApiResponse<RateLimitStatsResponse>> getRateLimitStats() {
        return ResponseEntity.ok(ApiResponse.success("Rate Limit 조회 성공",
                RateLimitStatsResponse.from(riotRateLimiter.getStats())));
    }
}
