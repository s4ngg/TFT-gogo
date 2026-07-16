CREATE TABLE patch_note_change_tombstones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    source_key VARCHAR(150) NOT NULL,
    deleted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_note_change_tombstones_source (patch_note_id, source_key),
    CONSTRAINT fk_patch_note_change_tombstones_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
