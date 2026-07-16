package com.tftgogo.domain.content.service;

import com.tftgogo.domain.content.dto.response.ContentRefreshHealthResponse;
import com.tftgogo.domain.content.entity.ContentRefreshFailureType;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;

public interface ContentRefreshMonitoringService {

    void recordAttempt(ContentRefreshJobType jobType);

    void recordSuccess(ContentRefreshJobType jobType, String version, long processedCount);

    void recordFailure(ContentRefreshJobType jobType, Throwable failure);

    void recordPartialSuccess(
            ContentRefreshJobType jobType,
            String version,
            long processedCount,
            ContentRefreshFailureType failureType
    );

    ContentRefreshHealthResponse getHealth();
}
