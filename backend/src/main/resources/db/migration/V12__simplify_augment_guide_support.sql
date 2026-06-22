-- Align augment guide support with the simplified ERD/UI contract.
-- New databases get this shape from V10; this migration adjusts databases
-- that already applied the previous augment guide schema.

SET NAMES utf8mb4;

ALTER TABLE tft_guide_augments
    DROP INDEX idx_tft_guide_augments_patch_tier,
    DROP COLUMN tier,
    DROP COLUMN type,
    DROP COLUMN reward,
    ADD INDEX idx_tft_guide_augments_patch_name (patch_version, name, id);

DROP TABLE IF EXISTS augment_guide_rewards;
DROP TABLE IF EXISTS augment_guide_plans;
