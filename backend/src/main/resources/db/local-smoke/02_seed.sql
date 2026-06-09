-- Minimal local smoke data for Guide and PatchNotes.
-- Safe to re-run: guide rows are upserted and local patch changes are replaced.

SET NAMES utf8mb4;

INSERT INTO guides (
    guide_type,
    target_key,
    name,
    summary,
    image_url,
    data_json,
    patch_version,
    sort_order,
    is_active
) VALUES
(
    'TRAIT',
    'local-smoke-trait',
    'Local Trait',
    'Local smoke test trait guide.',
    NULL,
    JSON_OBJECT(
        'count', 4,
        'type', 'Flex',
        'summary', 'Sample trait data for public guide smoke testing.',
        'tone', 'gold',
        'levels', JSON_ARRAY('2', '4', '6'),
        'tips', JSON_ARRAY('Check tab rendering and search flow.'),
        'champions', JSON_ARRAY(JSON_OBJECT('name', 'Local Champion', 'cost', 4, 'imageUrl', NULL))
    ),
    '17.3',
    10,
    1
),
(
    'ITEM',
    'local-smoke-item',
    'Local Item',
    'Local smoke test item guide.',
    NULL,
    JSON_OBJECT(
        'category', 'attack',
        'components', JSON_ARRAY('B.F. Sword', 'Recurve Bow'),
        'recommendedFor', JSON_ARRAY('Attack carry'),
        'stats', JSON_OBJECT('ad', '+10%', 'attackSpeed', '+10%'),
        'tips', JSON_ARRAY('Check item tab rendering and search flow.')
    ),
    '17.3',
    20,
    1
),
(
    'AUGMENT',
    'local-smoke-augment',
    'Local Augment',
    'Local smoke test augment guide.',
    NULL,
    JSON_OBJECT(
        'tier', 'gold',
        'stage', '2-1',
        'recommendedComps', JSON_ARRAY('Flex'),
        'tips', JSON_ARRAY('Check augment tab rendering and search flow.')
    ),
    '17.3',
    30,
    1
),
(
    'CHAMPION',
    'local-smoke-champion',
    'Local Champion',
    'Local smoke test champion guide.',
    NULL,
    JSON_OBJECT(
        'cost', 4,
        'role', 'AP Carry',
        'position', 'Backline',
        'traits', JSON_ARRAY('Local Trait'),
        'bestItems', JSON_ARRAY('Local Item'),
        'stats', JSON_OBJECT('ad', 50, 'armor', 30, 'attackSpeed', 0.75, 'hp', 900, 'mana', 80, 'mr', 30, 'range', 4),
        'ability', JSON_OBJECT('name', 'Local Skill', 'description', 'Smoke test ability description.', 'iconUrl', NULL)
    ),
    '17.3',
    40,
    1
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    summary = VALUES(summary),
    image_url = VALUES(image_url),
    data_json = VALUES(data_json),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    deleted_at = NULL;

INSERT INTO patch_notes (
    version,
    title,
    summary,
    description,
    focus,
    image_url,
    published_at,
    is_current,
    highlights_json,
    is_active
) VALUES (
    '17.3',
    '17.3 Local Patch',
    'Local smoke test patch note.',
    'Minimal data for checking Guide/PatchNotes public APIs without frontend fallback.',
    'Local smoke',
    NULL,
    '2026-06-09 00:00:00',
    1,
    JSON_ARRAY('Guide and PatchNotes smoke data added', 'Public APIs work without Riot API key'),
    1
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    description = VALUES(description),
    focus = VALUES(focus),
    image_url = VALUES(image_url),
    published_at = VALUES(published_at),
    is_current = VALUES(is_current),
    highlights_json = VALUES(highlights_json),
    is_active = VALUES(is_active),
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
    sort_order,
    is_active
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
    10,
    1
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
    20,
    1
);
