-- Store generated or summon-style units that belong under a trait card.

SET NAMES utf8mb4;
SET @schema_name = DATABASE();

SET @add_special_units_column = (
    SELECT IF(
        COUNT(*) = 0,
        "ALTER TABLE tft_guide_traits ADD COLUMN special_units_json JSON NULL AFTER champions_json",
        "SELECT 1"
    )
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'tft_guide_traits'
      AND column_name = 'special_units_json'
);
PREPARE stmt FROM @add_special_units_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE tft_guide_traits
SET special_units_json = JSON_ARRAY()
WHERE special_units_json IS NULL;

ALTER TABLE tft_guide_traits
    MODIFY COLUMN special_units_json JSON NOT NULL;
