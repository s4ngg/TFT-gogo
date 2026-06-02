-- Optional local smoke-test reset.
-- This removes only the seed rows created by guide-patchnote-local-seed.sql.
-- Run this against the tftgogo MySQL database only when you want to clean the local smoke data.

START TRANSACTION;

SET @patch_note_173_id = (
    SELECT id
    FROM patch_notes
    WHERE version = '17.3'
    LIMIT 1
);

DELETE FROM patch_changes
WHERE patch_note_id = @patch_note_173_id
  AND target_key IN (
      'TFT17_Jinx',
      'TFT17_DarkStar',
      'TFT_Item_SpearOfShojin',
      'TFT17_Augment_Guidebook',
      'system_shop_refresh'
  );

DELETE FROM patch_notes
WHERE id = @patch_note_173_id
  AND NOT EXISTS (
      SELECT 1
      FROM patch_changes
      WHERE patch_note_id = @patch_note_173_id
  );

DELETE FROM guides
WHERE patch_version = '17.3'
  AND target_key IN (
      'TFT17_DarkStar',
      'TFT_Item_SpearOfShojin',
      'TFT17_Augment_Guidebook',
      'TFT17_Jinx'
  );

COMMIT;

-- Local schema rollback, only if these tables contain no team or user data:
-- DROP TABLE IF EXISTS patch_changes;
-- DROP TABLE IF EXISTS patch_notes;
-- DROP TABLE IF EXISTS guides;
