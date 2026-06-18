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
    title VARCHAR(200) NOT NULL,
    summary TEXT NULL,
    focus VARCHAR(200) NULL,
    content MEDIUMTEXT NOT NULL,
    highlights_json JSON NULL,
    representative_image_url VARCHAR(500) NULL,
    source_key VARCHAR(150) NULL,
    source_url VARCHAR(500) NULL,
    import_source VARCHAR(30) NULL,
    source_locale VARCHAR(20) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    imported_at DATETIME(6) NULL,
    published_at DATETIME(6) NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_notes_version (version),
    UNIQUE KEY uk_patch_notes_source_key (source_key),
    UNIQUE KEY uk_patch_notes_source_url (source_url),
    KEY idx_patch_notes_public (deleted_at, is_current, published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_note_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    source_key VARCHAR(150) NULL,
    source_heading_path VARCHAR(500) NULL,
    source_order INT NULL,
    imported_at DATETIME(6) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    category VARCHAR(20) NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    impact VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    target_key VARCHAR(100) NULL,
    target_name VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    image_url VARCHAR(500) NULL,
    tags_json JSON NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_patch_note_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id) ON DELETE RESTRICT,
    UNIQUE KEY uk_patch_note_changes_source_key (patch_note_id, source_key),
    KEY idx_patch_note_changes_public (patch_note_id, sort_order, id),
    KEY idx_patch_note_changes_source_order (patch_note_id, source_order),
    KEY idx_patch_note_changes_filters (patch_note_id, category, change_type, impact)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
