-- PR #491: item/augment guide stat metrics are no longer exposed.
-- Keep V10 immutable after #483 and express the schema cleanup as a follow-up migration.

ALTER TABLE tft_guide_items
    DROP COLUMN stats_json;

ALTER TABLE tft_guide_augments
    DROP COLUMN stats_json;