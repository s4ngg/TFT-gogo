package com.tftgogo.domain.match.dto.response;

import lombok.Getter;

@Getter
public class CacheStatsResponse {
    private final long totalCount;
    private final long rankedCount;
    private final long normalCount;
    private final Long newestMatchTimestamp;
    private final Long oldestMatchTimestamp;
    private final String lastCachedAt;

    private CacheStatsResponse(long totalCount, long rankedCount, long normalCount,
                                Long newestMatchTimestamp, Long oldestMatchTimestamp,
                                String lastCachedAt) {
        this.totalCount = totalCount;
        this.rankedCount = rankedCount;
        this.normalCount = normalCount;
        this.newestMatchTimestamp = newestMatchTimestamp;
        this.oldestMatchTimestamp = oldestMatchTimestamp;
        this.lastCachedAt = lastCachedAt;
    }

    public static CacheStatsResponse of(long totalCount, long rankedCount, long normalCount,
                                        Long newestMatchTimestamp, Long oldestMatchTimestamp,
                                        String lastCachedAt) {
        return new CacheStatsResponse(totalCount, rankedCount, normalCount,
                newestMatchTimestamp, oldestMatchTimestamp, lastCachedAt);
    }
}
