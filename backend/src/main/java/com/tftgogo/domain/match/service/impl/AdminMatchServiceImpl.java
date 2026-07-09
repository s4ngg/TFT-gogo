package com.tftgogo.domain.match.service.impl;

import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.match.service.AdminMatchService;
import com.tftgogo.global.riot.RiotRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AdminMatchServiceImpl implements AdminMatchService {

    private static final int RANKED_QUEUE_ID = 1100;
    private static final int NORMAL_QUEUE_ID = 1090;

    private static final DateTimeFormatter KST_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final CachedMatchRepository cachedMatchRepository;
    private final RiotRateLimiter riotRateLimiter;

    @Override
    public CacheStatsResponse getCacheStats() {
        return CacheStatsResponse.of(
                cachedMatchRepository.count(),
                cachedMatchRepository.countByQueueId(RANKED_QUEUE_ID),
                cachedMatchRepository.countByQueueId(NORMAL_QUEUE_ID),
                cachedMatchRepository.findMaxGameDatetime().orElse(null),
                cachedMatchRepository.findMinGameDatetime().orElse(null),
                cachedMatchRepository.findLatestCachedAt()
                        .map(dt -> dt.atZone(ZoneId.of("Asia/Seoul")).format(KST_FMT))
                        .orElse(null)
        );
    }

    @Override
    public RateLimitStatsResponse getRateLimitStats() {
        return RateLimitStatsResponse.from(riotRateLimiter.getStats());
    }
}
