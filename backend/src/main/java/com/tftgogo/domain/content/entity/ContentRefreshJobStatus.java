package com.tftgogo.domain.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_refresh_job_statuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentRefreshJobStatus {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private ContentRefreshJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", nullable = false, length = 20)
    private ContentRefreshRunStatus lastStatus;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "last_success_version", length = 20)
    private String lastSuccessVersion;

    @Column(name = "last_success_count")
    private Long lastSuccessCount;

    @Column(name = "consecutive_failure_count", nullable = false)
    private int consecutiveFailureCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_failure_type", length = 30)
    private ContentRefreshFailureType lastFailureType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ContentRefreshJobStatus(ContentRefreshJobType jobType) {
        this.jobType = jobType;
        this.lastStatus = ContentRefreshRunStatus.NEVER_RUN;
    }

    public static ContentRefreshJobStatus create(ContentRefreshJobType jobType) {
        if (jobType == null) {
            throw new IllegalArgumentException("Content refresh job type is required");
        }
        return new ContentRefreshJobStatus(jobType);
    }

    public void recordAttempt(LocalDateTime attemptedAt) {
        this.lastStatus = ContentRefreshRunStatus.RUNNING;
        this.lastAttemptAt = requireTime(attemptedAt);
    }

    public void recordSuccess(String version, long processedCount, LocalDateTime succeededAt) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Content refresh success version is required");
        }
        if (processedCount < 0) {
            throw new IllegalArgumentException("Content refresh success count cannot be negative");
        }
        this.lastStatus = ContentRefreshRunStatus.SUCCESS;
        this.lastSuccessAt = requireTime(succeededAt);
        this.lastSuccessVersion = version.trim();
        this.lastSuccessCount = processedCount;
        this.consecutiveFailureCount = 0;
    }

    public void recordFailure(ContentRefreshFailureType failureType, LocalDateTime failedAt) {
        if (failureType == null) {
            throw new IllegalArgumentException("Content refresh failure type is required");
        }
        this.lastStatus = ContentRefreshRunStatus.FAILURE;
        this.lastFailureAt = requireTime(failedAt);
        this.lastFailureType = failureType;
        this.consecutiveFailureCount++;
    }

    public void recordPartialSuccess(
            String version,
            long processedCount,
            ContentRefreshFailureType failureType,
            LocalDateTime occurredAt
    ) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Content refresh partial success version is required");
        }
        if (processedCount < 0) {
            throw new IllegalArgumentException("Content refresh partial success count cannot be negative");
        }
        if (failureType == null) {
            throw new IllegalArgumentException("Content refresh partial failure type is required");
        }
        LocalDateTime timestamp = requireTime(occurredAt);
        this.lastStatus = ContentRefreshRunStatus.FAILURE;
        this.lastSuccessAt = timestamp;
        this.lastSuccessVersion = version.trim();
        this.lastSuccessCount = processedCount;
        this.lastFailureAt = timestamp;
        this.lastFailureType = failureType;
        this.consecutiveFailureCount++;
    }

    private LocalDateTime requireTime(LocalDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException("Content refresh timestamp is required");
        }
        return value;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (lastStatus == null) {
            lastStatus = ContentRefreshRunStatus.NEVER_RUN;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
