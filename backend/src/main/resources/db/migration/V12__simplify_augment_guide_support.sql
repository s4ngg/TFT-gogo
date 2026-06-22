-- Align augment guide support with the simplified ERD/UI contract.
-- New databases get the final shape from V10. Existing databases that applied
-- the previous augment guide schema are adjusted conditionally here.

SET NAMES utf8mb4;
SET @schema_name = DATABASE();

SET @drop_patch_tier_index = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE tft_guide_augments DROP INDEX idx_tft_guide_augments_patch_tier',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_augments'
      AND index_name = 'idx_tft_guide_augments_patch_tier'
);
PREPARE stmt FROM @drop_patch_tier_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_tier_column = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE tft_guide_augments DROP COLUMN tier',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_augments'
      AND column_name = 'tier'
);
PREPARE stmt FROM @drop_tier_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_type_column = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE tft_guide_augments DROP COLUMN type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_augments'
      AND column_name = 'type'
);
PREPARE stmt FROM @drop_type_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_reward_column = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE tft_guide_augments DROP COLUMN reward',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_augments'
      AND column_name = 'reward'
);
PREPARE stmt FROM @drop_reward_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_patch_name_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE tft_guide_augments ADD INDEX idx_tft_guide_augments_patch_name (patch_version, name, id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_augments'
      AND index_name = 'idx_tft_guide_augments_patch_name'
);
PREPARE stmt FROM @add_patch_name_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS augment_guide_rewards;
DROP TABLE IF EXISTS augment_guide_plans;
