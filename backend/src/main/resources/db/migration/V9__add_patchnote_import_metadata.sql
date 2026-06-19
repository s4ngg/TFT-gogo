-- PR: prepare patch note official crawl import metadata.
-- Manual apply is required because local JPA ddl-auto is none.

ALTER TABLE patch_notes
    ADD COLUMN source_url VARCHAR(500) NULL AFTER image_url,
    ADD COLUMN source_locale VARCHAR(20) NULL AFTER source_url,
    ADD COLUMN import_source VARCHAR(30) NULL AFTER source_locale,
    ADD COLUMN imported_at DATETIME(6) NULL AFTER import_source,
    ADD COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER imported_at,
    ADD INDEX idx_patch_notes_import_source (import_source, source_locale, imported_at);

ALTER TABLE patch_changes
    ADD COLUMN source_key VARCHAR(64) NULL AFTER patch_note_id,
    ADD COLUMN source_url VARCHAR(500) NULL AFTER source_key,
    ADD COLUMN source_heading_path VARCHAR(500) NULL AFTER source_url,
    ADD COLUMN source_order INT NULL AFTER source_heading_path,
    ADD COLUMN source_locale VARCHAR(20) NULL AFTER source_order,
    ADD COLUMN import_source VARCHAR(30) NULL AFTER source_locale,
    ADD COLUMN imported_at DATETIME(6) NULL AFTER import_source,
    ADD COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER imported_at,
    ADD UNIQUE INDEX uk_patch_changes_source_key (patch_note_id, source_key),
    ADD INDEX idx_patch_changes_import_source (import_source, source_locale, imported_at);
