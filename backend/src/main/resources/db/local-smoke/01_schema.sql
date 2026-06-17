-- Local smoke schema for guide and patch-note public/admin APIs.
-- Schema management is manual in this project because JPA ddl-auto is none
-- and no Flyway/Liquibase dependency is configured.

SET NAMES utf8mb4;

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
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guides_type_target_patch (guide_type, target_key, patch_version),
    KEY idx_guides_public_patch (patch_version, is_active, deleted_at, sort_order, id),
    KEY idx_guides_public_type_patch (guide_type, patch_version, is_active, deleted_at, sort_order, id),
    KEY idx_guides_admin (patch_version, guide_type, is_active, deleted_at, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NULL,
    focus VARCHAR(200) NULL,
    image_url VARCHAR(500) NULL,
    source_url VARCHAR(500) NULL,
    source_locale VARCHAR(20) NULL,
    import_source VARCHAR(30) NULL,
    imported_at DATETIME(6) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME(6) NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    highlights_json JSON NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    current_active_key TINYINT GENERATED ALWAYS AS (
        CASE
            WHEN is_current = 1 AND is_active = 1 AND deleted_at IS NULL THEN 1
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_notes_version (version),
    UNIQUE KEY uk_patch_notes_single_current_active (current_active_key),
    KEY idx_patch_notes_public (is_active, deleted_at, is_current, published_at, id),
    KEY idx_patch_notes_import_source (import_source, source_locale, imported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    source_key VARCHAR(64) NULL,
    source_url VARCHAR(500) NULL,
    source_heading_path VARCHAR(500) NULL,
    source_order INT NULL,
    source_locale VARCHAR(20) NULL,
    import_source VARCHAR(30) NULL,
    imported_at DATETIME(6) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
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
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_patch_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id),
    UNIQUE KEY uk_patch_changes_source_key (patch_note_id, source_key),
    KEY idx_patch_changes_public (patch_note_id, is_active, deleted_at, sort_order, id),
    KEY idx_patch_changes_filters (patch_note_id, category, change_type, impact, is_active, deleted_at),
    KEY idx_patch_changes_import_source (import_source, source_locale, imported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
