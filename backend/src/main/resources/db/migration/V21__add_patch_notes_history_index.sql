SET @has_patch_notes_history_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'patch_notes'
      AND index_name = 'idx_patch_notes_history'
);

SET @add_patch_notes_history_index := IF(
    @has_patch_notes_history_index = 0,
    'CREATE INDEX idx_patch_notes_history ON patch_notes (deleted_at, published_at, id)',
    'SELECT 1'
);

PREPARE add_patch_notes_history_index_statement FROM @add_patch_notes_history_index;
EXECUTE add_patch_notes_history_index_statement;
DEALLOCATE PREPARE add_patch_notes_history_index_statement;
