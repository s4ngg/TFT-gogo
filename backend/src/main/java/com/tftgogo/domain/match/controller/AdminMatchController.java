package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.AdminMatchControllerDocs;
import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.global.response.ApiResponse;
import com.tftgogo.global.riot.RiotRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/match")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'ADMIN_EDITOR', 'ADMIN_VIEWER')")
public class AdminMatchController implements AdminMatchControllerDocs {

    private static final int RANKED_QUEUE_ID = 1100;
    private static final int NORMAL_QUEUE_ID = 1090;

    private static final DateTimeFormatter KST_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CachedMatchRepository cachedMatchRepository;
    private final RiotRateLimiter riotRateLimiter;

    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats() {
        CacheStatsResponse stats = CacheStatsResponse.of(
                cachedMatchRepository.count(),
                cachedMatchRepository.countByQueueId(RANKED_QUEUE_ID),
                cachedMatchRepository.countByQueueId(NORMAL_QUEUE_ID),
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
