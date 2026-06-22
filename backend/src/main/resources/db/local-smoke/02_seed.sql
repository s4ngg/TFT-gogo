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

DELETE FROM augment_guide_rewards
WHERE patch_version = '17.3';

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

INSERT INTO augment_guide_rewards (
    stage,
    condition_text,
    reward_text,
    patch_version
) VALUES
('2-1', '실버 증강', '초반 체력과 필드 안정성을 우선 확인', '17.3'),
('2-1', '골드 증강', '초반 전투력 또는 경제 기반 확보', '17.3'),
('2-1', '프리즘 증강', '첫 방향성을 강하게 정하되 아이템 호환성 확인', '17.3'),
('3-2', '실버 증강', '현재 조합의 부족한 전투 능력 보강', '17.3'),
('3-2', '골드 증강', '핵심 시너지, 아이템, 경제 중 가장 부족한 축 보완', '17.3'),
('3-2', '프리즘 증강', '중반 전환 또는 리롤 완성 타이밍 확정', '17.3'),
('4-2', '실버 증강', '최종 조합의 약점 보완', '17.3'),
('4-2', '골드 증강', '캐리 화력 또는 앞라인 유지력 강화', '17.3'),
('4-2', '프리즘 증강', '최종 고점과 순방 안정성을 함께 확보', '17.3');

START TRANSACTION;

UPDATE patch_notes
SET is_current = 0
WHERE is_current = 1
  AND is_active = 1
  AND deleted_at IS NULL
  AND version <> '17.3';

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

COMMIT;
