package com.tftgogo.global.riot;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotRateLimiterTest {

    private RiotRateLimiter limiter(int shortMax, long shortWindowMs,
                                    int longMax, long longWindowMs, long maxWaitMs) {
        RiotProperties properties = new RiotProperties();
        properties.setApiKey("test-key");
        properties.setShortRateLimitMax(shortMax);
        properties.setShortRateLimitWindowMs(shortWindowMs);
        properties.setLongRateLimitMax(longMax);
        properties.setLongRateLimitWindowMs(longWindowMs);
        properties.setRateLimitMaxWaitMs(maxWaitMs);
        return new RiotRateLimiter(properties);
    }

    @Test
    void 토큰이_있으면_즉시_통과한다() throws InterruptedException {
        RiotRateLimiter rateLimiter = limiter(20, 1_000L, 100, 120_000L, 3_000L);

        long start = System.currentTimeMillis();
        rateLimiter.acquire();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(100);
    }

    @Test
    void 버킷_고갈_상태에서_내부_대기_한도를_초과하면_RATE_LIMIT_예외로_즉시_실패한다() throws InterruptedException {
        // 단기 버킷 1개, 윈도우 60초(테스트 중 리필 안 됨) + 내부 대기 한도 150ms
        RiotRateLimiter rateLimiter = limiter(1, 60_000L, 100, 120_000L, 150L);
        rateLimiter.acquire(); // 유일한 토큰 소진

        long start = System.currentTimeMillis();
        assertThatThrownBy(rateLimiter::acquire)
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RIOT_API_RATE_LIMIT);
                    assertThat(e.getRetryAfterSeconds()).isGreaterThan(0);
                });
        long elapsed = System.currentTimeMillis() - start;

        // 리필 윈도우(60초)까지 블로킹되는 게 아니라 maxWaitMs(150ms) 근방에서 실패해야 한다
        assertThat(elapsed).isLessThan(1_000);
    }

    @Test
    void 대기_중_토큰이_리필되면_한도_내에서_정상_통과한다() throws InterruptedException {
        // 단기 버킷 1개, 윈도우 100ms(금방 리필) + 내부 대기 한도 3초
        RiotRateLimiter rateLimiter = limiter(1, 100L, 100, 120_000L, 3_000L);
        rateLimiter.acquire(); // 유일한 토큰 소진

        long start = System.currentTimeMillis();
        rateLimiter.acquire(); // 윈도우 리필 대기 후 통과
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(3_000);
    }
}
