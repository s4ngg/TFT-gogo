package com.tftgogo.domain.content.dto.response;

import com.tftgogo.domain.content.entity.ContentRefreshFailureType;
import com.tftgogo.domain.content.entity.ContentRefreshJobType;
import com.tftgogo.domain.content.entity.ContentRefreshRunStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ContentRefreshHealthResponse {

    private final String status;
    private final LocalDateTime checkedAt;
    private final long staleAfterHours;
    private final int consecutiveFailureCriticalThreshold;
    private final long runningTimeoutMinutes;
    private final List<JobHealth> jobs;

    public static ContentRefreshHealthResponse of(
            String status,
            LocalDateTime checkedAt,
            long staleAfterHours,
            int consecutiveFailureCriticalThreshold,
            long runningTimeoutMinutes,
            List<JobHealth> jobs
    ) {
        return ContentRefreshHealthResponse.builder()
                .status(status)
                .checkedAt(checkedAt)
                .staleAfterHours(staleAfterHours)
                .consecutiveFailureCriticalThreshold(consecutiveFailureCriticalThreshold)
                .runningTimeoutMinutes(runningTimeoutMinutes)
                .jobs(List.copyOf(jobs))
                .build();
    }

    @Getter
    @Builder
    public static class JobHealth {

        private final ContentRefreshJobType jobType;
        private final boolean enabled;
        private final String status;
        private final List<String> alertReasons;
        private final ContentRefreshRunStatus lastStatus;
        private final LocalDateTime lastAttemptAt;
        private final LocalDateTime lastSuccessAt;
        private final LocalDateTime lastFailureAt;
        private final String lastSuccessVersion;
        private final Long lastSuccessCount;
        private final int consecutiveFailureCount;
        private final ContentRefreshFailureType lastFailureType;
        private final boolean fresh;
        private final String currentVersion;
        private final Long currentCount;
        private final boolean currentDataComplete;

        public static JobHealth of(
                ContentRefreshJobType jobType,
                boolean enabled,
                String status,
                List<String> alertReasons,
                ContentRefreshRunStatus lastStatus,
                LocalDateTime lastAttemptAt,
                LocalDateTime lastSuccessAt,
                LocalDateTime lastFailureAt,
                String lastSuccessVersion,
                Long lastSuccessCount,
                int consecutiveFailureCount,
                ContentRefreshFailureType lastFailureType,
                boolean fresh,
                String currentVersion,
                Long currentCount,
                boolean currentDataComplete
        ) {
            return JobHealth.builder()
                    .jobType(jobType)
                    .enabled(enabled)
                    .status(status)
                    .alertReasons(List.copyOf(alertReasons))
                    .lastStatus(lastStatus)
                    .lastAttemptAt(lastAttemptAt)
                    .lastSuccessAt(lastSuccessAt)
                    .lastFailureAt(lastFailureAt)
                    .lastSuccessVersion(lastSuccessVersion)
                    .lastSuccessCount(lastSuccessCount)
                    .consecutiveFailureCount(consecutiveFailureCount)
                    .lastFailureType(lastFailureType)
                    .fresh(fresh)
                    .currentVersion(currentVersion)
                    .currentCount(currentCount)
                    .currentDataComplete(currentDataComplete)
                    .build();
        }
    }
}
