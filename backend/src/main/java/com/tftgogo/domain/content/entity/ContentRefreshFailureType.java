package com.tftgogo.domain.content.entity;

public enum ContentRefreshFailureType {
    SOURCE_UNAVAILABLE,
    INVALID_DATA,
    VALIDATION,
    STORAGE,
    CONCURRENCY,
    HISTORY_BACKFILL,
    UNEXPECTED
}
