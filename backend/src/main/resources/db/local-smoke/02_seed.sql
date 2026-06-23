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
    '17.3'
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
    '17.3'
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
    '17.3'
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
    '17.3'
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

UPDATE patch_notes
SET is_current = 0
WHERE is_current = 1
  AND deleted_at IS NULL
  AND version <> '17.3';

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
    '17.3',
    '17.3 Local Patch',
    'Local smoke test patch note.',
    'Minimal data for checking Guide/PatchNotes public APIs without frontend fallback.',
    'Local smoke',
    NULL,
    '2026-06-09 00:00:00',
    1,
    JSON_ARRAY('Guide and PatchNotes smoke data added', 'Public APIs work without Riot API key')
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    content = VALUES(content),
    focus = VALUES(focus),
    representative_image_url = VALUES(representative_image_url),
    published_at = VALUES(published_at),
    is_current = VALUES(is_current),
    highlights_json = VALUES(highlights_json),
    deleted_at = NULL;

DELETE pc
FROM patch_changes pc
JOIN patch_notes pn ON pn.id = pc.patch_note_id
WHERE pn.version = '17.3'
  AND pc.target_key LIKE 'local-smoke-%';

INSERT INTO patch_changes (
    patch_note_id,
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
) VALUES
(
    (SELECT id FROM patch_notes WHERE version = '17.3'),
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
),
(
    (SELECT id FROM patch_notes WHERE version = '17.3'),
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
);

COMMIT;
