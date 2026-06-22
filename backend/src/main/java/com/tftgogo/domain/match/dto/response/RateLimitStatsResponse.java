package com.tftgogo.domain.match.dto.response;

import com.tftgogo.global.riot.RiotRateLimiter;
import lombok.Getter;

@Getter
public class RateLimitStatsResponse {
    private final int shortRemaining;
    private final int shortMax;
    private final long shortWindowMs;
    private final long shortWindowRemainMs;
    private final int longRemaining;
    private final int longMax;
    private final long longWindowMs;
    private final long longWindowRemainMs;

    private RateLimitStatsResponse(int shortRemaining, int shortMax, long shortWindowMs, long shortWindowRemainMs,
                                   int longRemaining, int longMax, long longWindowMs, long longWindowRemainMs) {
        this.shortRemaining = shortRemaining;
        this.shortMax = shortMax;
        this.shortWindowMs = shortWindowMs;
        this.shortWindowRemainMs = shortWindowRemainMs;
        this.longRemaining = longRemaining;
        this.longMax = longMax;
        this.longWindowMs = longWindowMs;
        this.longWindowRemainMs = longWindowRemainMs;
    }

    public static RateLimitStatsResponse from(RiotRateLimiter.RateLimitSnapshot snapshot) {
        return new RateLimitStatsResponse(
                snapshot.shortRemaining(), snapshot.shortMax(),
                snapshot.shortWindowMs(), snapshot.shortWindowRemainMs(),
                snapshot.longRemaining(), snapshot.longMax(),
                snapshot.longWindowMs(), snapshot.longWindowRemainMs()
        );
    }
}
