-- PR #433: align the existing patch-note schema with the team ERD.
-- Keep earlier migration files immutable; schema replacements are expressed here.

UPDATE patch_notes
SET description = COALESCE(description, summary, '')
WHERE description IS NULL;

ALTER TABLE patch_notes
    DROP INDEX uk_patch_notes_single_current_active,
    DROP INDEX idx_patch_notes_import_source,
    ADD COLUMN source_key VARCHAR(150) NULL AFTER image_url,
    CHANGE COLUMN image_url representative_image_url VARCHAR(500) NULL,
    CHANGE COLUMN description content MEDIUMTEXT NOT NULL,
    MODIFY COLUMN title VARCHAR(200) NOT NULL,
    MODIFY COLUMN summary TEXT NULL,
    MODIFY COLUMN source_url VARCHAR(500) NULL AFTER source_key,
    MODIFY COLUMN import_source VARCHAR(30) NULL AFTER source_url,
    MODIFY COLUMN source_locale VARCHAR(20) NULL AFTER import_source,
    MODIFY COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER source_locale,
    MODIFY COLUMN imported_at DATETIME(6) NULL AFTER manually_edited,
    DROP COLUMN current_active_key,
    DROP COLUMN is_active;

ALTER TABLE patch_changes
    RENAME TO patch_note_changes;

ALTER TABLE patch_note_changes
    DROP FOREIGN KEY fk_patch_changes_patch_note,
    DROP INDEX uk_patch_changes_source_key,
    DROP INDEX idx_patch_changes_public,
    DROP INDEX idx_patch_changes_filters,
    DROP INDEX idx_patch_changes_import_source,
    MODIFY COLUMN source_key VARCHAR(150) NULL AFTER patch_note_id,
    MODIFY COLUMN source_heading_path VARCHAR(500) NULL AFTER source_key,
    MODIFY COLUMN source_order INT NULL AFTER source_heading_path,
    MODIFY COLUMN imported_at DATETIME(6) NULL AFTER source_order,
    MODIFY COLUMN manually_edited TINYINT(1) NOT NULL DEFAULT 0 AFTER imported_at,
    MODIFY COLUMN impact VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    MODIFY COLUMN target_key VARCHAR(100) NULL,
    MODIFY COLUMN summary TEXT NOT NULL,
    MODIFY COLUMN before_value TEXT NULL,
    MODIFY COLUMN after_value TEXT NULL,
    DROP COLUMN source_url,
    DROP COLUMN source_locale,
    DROP COLUMN import_source,
    DROP COLUMN is_active,
    DROP COLUMN deleted_at,
    ADD CONSTRAINT fk_patch_note_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX uk_patch_notes_source_key
    ON patch_notes (source_key);

CREATE UNIQUE INDEX uk_patch_notes_source_url
    ON patch_notes (source_url);

CREATE UNIQUE INDEX uk_patch_note_changes_source_key
    ON patch_note_changes (patch_note_id, source_key);

CREATE UNIQUE INDEX uk_patch_notes_single_current
    ON patch_notes (
        (CASE
            WHEN is_current = 1 AND deleted_at IS NULL THEN 1
            ELSE NULL
        END)
    );

CREATE INDEX idx_patch_notes_public
    ON patch_notes (deleted_at, is_current, published_at, id);

CREATE INDEX idx_patch_note_changes_source_order
    ON patch_note_changes (patch_note_id, source_order);

CREATE INDEX idx_patch_note_changes_public
    ON patch_note_changes (patch_note_id, sort_order, id);

CREATE INDEX idx_patch_note_changes_filters
    ON patch_note_changes (patch_note_id, category, change_type, impact);
