ALTER TABLE tft_guide_snapshots
    ADD COLUMN source_set_number INT UNSIGNED NULL AFTER patch_version,
    ADD COLUMN source_mutator VARCHAR(100) NULL AFTER source_set_number,
    ADD CONSTRAINT chk_tft_guide_snapshots_source_pair
        CHECK (
            (source_set_number IS NULL AND source_mutator IS NULL)
            OR (
                source_set_number IS NOT NULL
                AND source_set_number > 0
                AND source_mutator IS NOT NULL
                AND CHAR_LENGTH(TRIM(source_mutator)) > 0
            )
        );
