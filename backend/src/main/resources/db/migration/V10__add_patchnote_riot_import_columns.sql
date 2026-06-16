ALTER TABLE patch_notes
    ADD COLUMN source_key VARCHAR(150) NULL AFTER representative_image_url,
    ADD COLUMN source_url VARCHAR(500) NULL AFTER source_key,
    ADD COLUMN import_source VARCHAR(30) NULL AFTER source_url,
    ADD COLUMN source_locale VARCHAR(20) NULL AFTER import_source,
    ADD COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER source_locale,
    ADD COLUMN imported_at DATETIME NULL AFTER manually_edited;

ALTER TABLE patch_note_changes
    ADD COLUMN source_key VARCHAR(150) NULL AFTER patch_note_id,
    ADD COLUMN source_heading_path VARCHAR(500) NULL AFTER source_key,
    ADD COLUMN source_order INT NULL AFTER source_heading_path,
    ADD COLUMN imported_at DATETIME NULL AFTER source_order,
    ADD COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER imported_at;

CREATE UNIQUE INDEX uk_patch_notes_source_key
    ON patch_notes (source_key);

CREATE UNIQUE INDEX uk_patch_notes_source_url
    ON patch_notes (source_url);

CREATE UNIQUE INDEX uk_patch_note_changes_source_key
    ON patch_note_changes (patch_note_id, source_key);

CREATE INDEX idx_patch_note_changes_source_order
    ON patch_note_changes (patch_note_id, source_order);
