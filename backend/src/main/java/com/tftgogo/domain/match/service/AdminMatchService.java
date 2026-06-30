package com.tftgogo.domain.match.service;

import com.tftgogo.domain.match.dto.response.CacheStatsResponse;
import com.tftgogo.domain.match.dto.response.RateLimitStatsResponse;

public interface AdminMatchService {

    CacheStatsResponse getCacheStats();

    RateLimitStatsResponse getRateLimitStats();
}
