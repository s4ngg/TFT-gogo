-- Local smoke-test schema for Guide and PatchNotes.
-- Run this against the tftgogo MySQL database before applying the seed file.
-- This is a manual SQL baseline until the team adopts Flyway, Liquibase, or another migration tool.

CREATE TABLE IF NOT EXISTS guides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_type VARCHAR(20) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    summary TEXT NULL,
    image_url VARCHAR(500) NULL,
    data_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_guides_type_key_patch UNIQUE (guide_type, target_key, patch_version),
    INDEX idx_guides_active_sort (is_active, deleted_at, sort_order, id),
    INDEX idx_guides_type_patch_active (guide_type, patch_version, is_active, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NULL,
    focus VARCHAR(200) NULL,
    image_url VARCHAR(500) NULL,
    published_at DATETIME(6) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    highlights_json JSON NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_patch_notes_version UNIQUE (version),
    INDEX idx_patch_notes_public_order (is_active, deleted_at, is_current, published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    impact VARCHAR(20) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    target_name VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    before_value VARCHAR(300) NULL,
    after_value VARCHAR(300) NULL,
    image_url VARCHAR(500) NULL,
    tags_json JSON NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_patch_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id),
    INDEX idx_patch_changes_note_sort (patch_note_id, is_active, deleted_at, sort_order, id),
    INDEX idx_patch_changes_note_filter (patch_note_id, is_active, deleted_at, category, change_type, impact),
    INDEX idx_patch_changes_target (target_key, target_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
