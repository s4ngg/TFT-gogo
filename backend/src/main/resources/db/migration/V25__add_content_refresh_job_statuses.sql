CREATE TABLE content_refresh_job_statuses (
    job_type VARCHAR(30) NOT NULL,
    last_status VARCHAR(20) NOT NULL DEFAULT 'NEVER_RUN',
    last_attempt_at DATETIME(6) NULL,
    last_success_at DATETIME(6) NULL,
    last_failure_at DATETIME(6) NULL,
    last_success_version VARCHAR(20) NULL,
    last_success_count BIGINT UNSIGNED NULL,
    consecutive_failure_count INT UNSIGNED NOT NULL DEFAULT 0,
    last_failure_type VARCHAR(30) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (job_type),
    CONSTRAINT chk_content_refresh_job_type
        CHECK (job_type IN ('PATCH_NOTE', 'GAME_GUIDE')),
    CONSTRAINT chk_content_refresh_last_status
        CHECK (last_status IN ('NEVER_RUN', 'RUNNING', 'SUCCESS', 'FAILURE')),
    CONSTRAINT chk_content_refresh_failure_type
        CHECK (
            last_failure_type IS NULL
            OR last_failure_type IN (
                'SOURCE_UNAVAILABLE',
                'INVALID_DATA',
                'VALIDATION',
                'STORAGE',
                'CONCURRENCY',
                'HISTORY_BACKFILL',
                'UNEXPECTED'
            )
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO content_refresh_job_statuses (job_type)
VALUES ('PATCH_NOTE'), ('GAME_GUIDE');
