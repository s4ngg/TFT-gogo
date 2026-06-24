package com.tftgogo.global.riot;

import com.tftgogo.global.riot.config.RiotProperties;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Riot Dev key 기준 이중 토큰 버킷 rate limiter.
 * 단기(20req/1s)와 장기(100req/2min) 두 버킷을 동시에 통과해야 요청이 진행된다.
 */
@Component
@RequiredArgsConstructor
public class RiotRateLimiter {

    private static final Logger logger = LogManager.getLogger(RiotRateLimiter.class);

    private final RiotProperties riotProperties;

    private final Object lock = new Object();
    private int shortTokens = -1;
    private int longTokens = -1;
    private long shortWindowStart;
    private long longWindowStart;

    public void acquire() throws InterruptedException {
        synchronized (lock) {
            if (shortTokens < 0) {
                shortTokens = riotProperties.getShortRateLimitMax();
                longTokens = riotProperties.getLongRateLimitMax();
                shortWindowStart = System.currentTimeMillis();
                longWindowStart = System.currentTimeMillis();
            }
            while (true) {
                refill();
                if (shortTokens > 0 && longTokens > 0) {
                    shortTokens--;
                    longTokens--;
                    return;
                }
                long waitMs = nextRefillMs();
                logger.debug("Rate limit 대기: {}ms (단기={}, 장기={})", waitMs, shortTokens, longTokens);
                lock.wait(waitMs);
            }
        }
    }

    private void refill() {
        long now = System.currentTimeMillis();
        if (now - shortWindowStart >= riotProperties.getShortRateLimitWindowMs()) {
            shortTokens = riotProperties.getShortRateLimitMax();
            shortWindowStart = now;
        }
        if (now - longWindowStart >= riotProperties.getLongRateLimitWindowMs()) {
            longTokens = riotProperties.getLongRateLimitMax();
            longWindowStart = now;
        }
    }

    private long nextRefillMs() {
        long now = System.currentTimeMillis();
        long shortWait = shortTokens == 0
                ? riotProperties.getShortRateLimitWindowMs() - (now - shortWindowStart)
                : Long.MAX_VALUE;
        long longWait = longTokens == 0
                ? riotProperties.getLongRateLimitWindowMs() - (now - longWindowStart)
                : Long.MAX_VALUE;
        return Math.max(1L, Math.min(shortWait, longWait));
    }

    public RateLimitSnapshot getStats() {
        synchronized (lock) {
            int shortMax = riotProperties.getShortRateLimitMax();
            int longMax = riotProperties.getLongRateLimitMax();
            long shortWindowMs = riotProperties.getShortRateLimitWindowMs();
            long longWindowMs = riotProperties.getLongRateLimitWindowMs();

            boolean initialized = shortTokens >= 0;
            if (initialized) refill();

            int shortRem = initialized ? Math.max(0, shortTokens) : shortMax;
            int longRem = initialized ? Math.max(0, longTokens) : longMax;

            long now = System.currentTimeMillis();
            long shortRemainMs = initialized ? Math.max(0, shortWindowMs - (now - shortWindowStart)) : 0;
            long longRemainMs = initialized ? Math.max(0, longWindowMs - (now - longWindowStart)) : 0;

            return new RateLimitSnapshot(
                    shortRem, shortMax, shortWindowMs, shortRemainMs,
                    longRem, longMax, longWindowMs, longRemainMs
            );
        }
    }

    public record RateLimitSnapshot(
            int shortRemaining, int shortMax, long shortWindowMs, long shortWindowRemainMs,
            int longRemaining, int longMax, long longWindowMs, long longWindowRemainMs
    ) {}
}
