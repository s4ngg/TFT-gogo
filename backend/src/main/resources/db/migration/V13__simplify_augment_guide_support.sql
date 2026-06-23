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

DELETE FROM augment_guide_plans
WHERE patch_version = '17.3';

INSERT INTO augment_guide_plans (
    plan_key,
    label,
    stages_json,
    patch_version
) VALUES
(
    'fast8',
    '빠른 8레벨',
    JSON_ARRAY(
        JSON_OBJECT('stage', '2-1', 'choice', '전투 유지 증강', 'focus', '초반 체력 보존'),
        JSON_OBJECT('stage', '3-2', 'choice', '경제 또는 핵심 시너지 보강', 'focus', '레벨업 기반 마련'),
        JSON_OBJECT('stage', '4-2', 'choice', '최종 조합 핵심 증강', 'focus', '8레벨 전환 완성')
    ),
    '17.3'
),
(
    'reroll',
    '리롤 운영',
    JSON_ARRAY(
        JSON_OBJECT('stage', '2-1', 'choice', '초반 기물 강화 증강', 'focus', '핵심 저코스트 확보'),
        JSON_OBJECT('stage', '3-2', 'choice', '리롤 효율 증강', 'focus', '3성 각 확인'),
        JSON_OBJECT('stage', '4-2', 'choice', '전투력 보강 증강', 'focus', '완성 조합 유지력 강화')
    ),
    '17.3'
),
(
    'flex',
    '유연한 운영',
    JSON_ARRAY(
        JSON_OBJECT('stage', '2-1', 'choice', '범용 전투 증강', 'focus', '초반 방향 열어두기'),
        JSON_OBJECT('stage', '3-2', 'choice', '아이템 또는 시너지 보강', 'focus', '보유 기물에 맞춰 전환'),
        JSON_OBJECT('stage', '4-2', 'choice', '고점 확장 증강', 'focus', '최종 캐리와 보조 시너지 확정')
    ),
    '17.3'
);
