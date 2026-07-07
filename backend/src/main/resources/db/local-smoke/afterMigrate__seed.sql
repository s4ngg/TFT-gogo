-- Minimal local smoke data for Guide, PatchNotes, and ELD reserved chat rooms.
-- Safe to re-run: rows are upserted or local namespaced patch changes are replaced.

SET NAMES utf8mb4;

-- Current MVP chat does not read chat_rooms. These rows document the fixed room ids
-- that CommunityChatRoomIds accepts and keep the ELD ready for a later persistent chat slice.
INSERT INTO chat_rooms (
    room_key,
    creator_id,
    party_post_id,
    name,
    type,
    is_active
) VALUES
('general', NULL, NULL, 'General', 'PUBLIC', 1),
('deck-guide', NULL, NULL, 'Deck Guide', 'PUBLIC', 1),
('party-recruitment', NULL, NULL, 'Party Recruitment', 'PUBLIC', 1),
('question-answer', NULL, NULL, 'Question Answer', 'PUBLIC', 1)
ON DUPLICATE KEY UPDATE
    creator_id = VALUES(creator_id),
    party_post_id = VALUES(party_post_id),
    name = VALUES(name),
    type = VALUES(type),
    is_active = VALUES(is_active);

INSERT INTO tft_guide_traits (
    trait_key,
    name,
    type,
    icon_url,
    tone,
    summary,
    levels_json,
    tier_effects_json,
    champions_json,
    special_units_json,
    tips_json,
    patch_version
) VALUES (
    'local-smoke-trait',
    'Local Trait',
    'Flex',
    'https://example.com/local-smoke-trait.png',
    'gold',
    'Sample trait data for public guide smoke testing.',
    JSON_ARRAY('2', '4', '6'),
    JSON_ARRAY(JSON_OBJECT('level', '2', 'description', 'Smoke trait effect.')),
    JSON_ARRAY(JSON_OBJECT('name', 'Local Champion', 'cost', 4, 'imageUrl', 'https://example.com/local-smoke-champion.png')),
    JSON_ARRAY(),
    JSON_ARRAY('Check tab rendering and search flow.'),
    '17.6'
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    icon_url = VALUES(icon_url),
    tone = VALUES(tone),
    summary = VALUES(summary),
    levels_json = VALUES(levels_json),
    tier_effects_json = VALUES(tier_effects_json),
    champions_json = VALUES(champions_json),
    special_units_json = VALUES(special_units_json),
    tips_json = VALUES(tips_json);

INSERT INTO tft_guide_items (
    item_key,
    name,
    category,
    image_url,
    description,
    stats_json,
    best_users_json,
    combinations_json,
    patch_version
) VALUES (
    'local-smoke-item',
    'Local Item',
    'attack',
    'https://example.com/local-smoke-item.png',
    'Local smoke test item guide.',
    JSON_OBJECT(),
    JSON_ARRAY(),
    JSON_ARRAY(JSON_OBJECT('label', 'Local combination', 'items', JSON_ARRAY('B.F. Sword', 'Recurve Bow'))),
    '17.6'
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    image_url = VALUES(image_url),
    description = VALUES(description),
    stats_json = VALUES(stats_json),
    best_users_json = VALUES(best_users_json),
    combinations_json = VALUES(combinations_json);

INSERT INTO tft_guide_augments (
    augment_key,
    name,
    description,
    icon_url,
    tags_json,
    stats_json,
    patch_version
) VALUES (
    'local-smoke-augment',
    'Local Augment',
    'Local smoke test augment guide.',
    'https://example.com/local-smoke-augment.png',
    JSON_ARRAY('Flex'),
    JSON_OBJECT(),
    '17.6'
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    icon_url = VALUES(icon_url),
    tags_json = VALUES(tags_json),
    stats_json = VALUES(stats_json);

INSERT INTO tft_guide_champions (
    champion_key,
    name,
    cost,
    role,
    position,
    image_url,
    stats_json,
    traits_json,
    best_items_json,
    patch_version
) VALUES (
    'local-smoke-champion',
    'Local Champion',
    4,
    'AP Carry',
    'Backline',
    'https://example.com/local-smoke-champion.png',
    JSON_OBJECT('ad', 50, 'armor', 30, 'attackSpeed', 0.75, 'hp', 900, 'mana', 80, 'mr', 30, 'range', 4),
    JSON_ARRAY('Local Trait'),
    JSON_ARRAY('Local Item'),
    '17.6'
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    cost = VALUES(cost),
    role = VALUES(role),
    position = VALUES(position),
    image_url = VALUES(image_url),
    stats_json = VALUES(stats_json),
    traits_json = VALUES(traits_json),
    best_items_json = VALUES(best_items_json);

START TRANSACTION;

SET @local_smoke_patch_version := _utf8mb4'17.6' COLLATE utf8mb4_unicode_ci;
SET @local_smoke_patch_imported := (
  SELECT COUNT(*) > 0
  FROM patch_notes
  WHERE deleted_at IS NULL
    AND version = @local_smoke_patch_version
    AND (import_source IS NOT NULL OR source_key IS NOT NULL OR source_url IS NOT NULL)
);

SET @has_real_patch_note := (
  SELECT COUNT(*) > 0
  FROM patch_notes
  WHERE deleted_at IS NULL
    AND version <> @local_smoke_patch_version
);

UPDATE patch_notes
SET is_current = 0
WHERE @has_real_patch_note
  AND deleted_at IS NULL
  AND version = @local_smoke_patch_version;

UPDATE patch_notes
SET is_current = 0
WHERE NOT @has_real_patch_note
  AND is_current = 1
  AND deleted_at IS NULL
  AND version <> @local_smoke_patch_version;

INSERT INTO patch_notes (
    version,
    title,
    summary,
    content,
    focus,
    representative_image_url,
    published_at,
    is_current,
    highlights_json
) VALUES (
    @local_smoke_patch_version,
    '17.6 Local Patch',
    'Local smoke test patch note.',
    'Minimal data for checking Guide/PatchNotes public APIs without frontend fallback.',
    'Local smoke',
    NULL,
    '2026-06-24 03:00:00',
    CASE WHEN @has_real_patch_note THEN 0 ELSE 1 END,
    JSON_ARRAY('Guide and PatchNotes smoke data added', 'Public APIs work without Riot API key')
)
ON DUPLICATE KEY UPDATE
    title = IF(@local_smoke_patch_imported, title, VALUES(title)),
    summary = IF(@local_smoke_patch_imported, summary, VALUES(summary)),
    content = IF(@local_smoke_patch_imported, content, VALUES(content)),
    focus = IF(@local_smoke_patch_imported, focus, VALUES(focus)),
    representative_image_url = IF(@local_smoke_patch_imported, representative_image_url, VALUES(representative_image_url)),
    published_at = IF(@local_smoke_patch_imported, published_at, VALUES(published_at)),
    is_current = CASE
        WHEN @local_smoke_patch_imported THEN is_current
        WHEN @has_real_patch_note THEN 0
        ELSE VALUES(is_current)
    END,
    highlights_json = IF(@local_smoke_patch_imported, highlights_json, VALUES(highlights_json)),
    deleted_at = NULL;

SET @has_real_current_patch_note := (
  SELECT COUNT(*) > 0
  FROM patch_notes
  WHERE deleted_at IS NULL
    AND is_current = 1
    AND version <> @local_smoke_patch_version
);

UPDATE patch_notes pn
JOIN (
  SELECT id
  FROM (
    SELECT id
    FROM patch_notes
    WHERE deleted_at IS NULL
      AND version <> @local_smoke_patch_version
    ORDER BY published_at DESC, id DESC
    LIMIT 1
  ) latest_real_patch_note
) latest ON latest.id = pn.id
SET pn.is_current = 1
WHERE @has_real_patch_note
  AND NOT @has_real_current_patch_note;

DELETE pc
FROM patch_note_changes pc
JOIN patch_notes pn ON pn.id = pc.patch_note_id
WHERE pn.version = @local_smoke_patch_version
  AND pc.target_key LIKE 'local-smoke-%';

INSERT INTO patch_note_changes (
    patch_note_id,
    source_key,
    source_heading_path,
    source_order,
    imported_at,
    category,
    change_type,
    impact,
    target_key,
    target_name,
    summary,
    before_value,
    after_value,
    image_url,
    tags_json,
    sort_order
)
SELECT
    (SELECT id FROM patch_notes WHERE version = @local_smoke_patch_version),
    'local-smoke-champion',
    'Local smoke',
    10,
    NOW(6),
    'CHAMPION',
    'BUFF',
    'HIGH',
    'local-smoke-champion',
    'Local Champion',
    'Check champion change rendering.',
    'Damage 100',
    'Damage 120',
    NULL,
    JSON_ARRAY('champion', 'buff'),
    10
WHERE NOT @local_smoke_patch_imported
UNION ALL
SELECT
    (SELECT id FROM patch_notes WHERE version = @local_smoke_patch_version),
    'local-smoke-trait',
    'Local smoke',
    20,
    NOW(6),
    'TRAIT',
    'ADJUST',
    'MEDIUM',
    'local-smoke-trait',
    'Local Trait',
    'Check trait change filters and stats.',
    '2/4',
    '2/4/6',
    NULL,
    JSON_ARRAY('trait', 'adjust'),
    20
WHERE NOT @local_smoke_patch_imported;

COMMIT;

-- 관리자 계정 (로컬 개발용) username=admin / password=changeme
-- BCrypt hash of 'changeme' (rounds=10)
INSERT IGNORE INTO admin_accounts (username, password, role, enabled)
VALUES ('admin', '$2a$10$uP.c0PzYAn7WBLAkItOKWuI9aZaGsi5OJaTZQdhlXJBMiUOr3Kpky', 'MASTER', 1);
