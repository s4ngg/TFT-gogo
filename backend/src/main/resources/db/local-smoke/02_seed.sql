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
    '전략가',
    '초반 운영 안정성을 높여주는 기본 시너지 가이드입니다.',
    NULL,
    JSON_OBJECT(
        'count', 4,
        'type', 'Flex',
        'summary', '전략가는 초반 방어 능력과 중반 전환 안정성을 보강하는 시너지입니다.',
        'tone', 'gold',
        'levels', JSON_ARRAY('2', '4', '6'),
        'tips', JSON_ARRAY('앞라인 유닛을 먼저 확보한 뒤 후방 캐리를 보강하세요.'),
        'champions', JSON_ARRAY(JSON_OBJECT('name', '아리', 'cost', 4, 'imageUrl', NULL))
    ),
    '17.3',
    10,
    1
),
(
    'ITEM',
    'local-smoke-item',
    '구인수의 격노검',
    '지속 전투 캐리에게 어울리는 공격 속도 아이템입니다.',
    NULL,
    JSON_OBJECT(
        'category', 'attack',
        'components', JSON_ARRAY('쓸데없이 큰 지팡이', '곡궁'),
        'recommendedFor', JSON_ARRAY('지속 전투 캐리'),
        'stats', JSON_OBJECT('ad', '+10%', 'attackSpeed', '+10%'),
        'tips', JSON_ARRAY('전투 시간이 길어지는 조합에서 효율이 좋습니다.')
    ),
    '17.3',
    20,
    1
),
(
    'AUGMENT',
    'local-smoke-augment',
    '전투 훈련',
    '주력 유닛의 성장 기대값을 높이는 기본 증강체입니다.',
    NULL,
    JSON_OBJECT(
        'tier', 'gold',
        'stage', '2-1',
        'recommendedComps', JSON_ARRAY('유연한 운영'),
        'tips', JSON_ARRAY('초반부터 필드를 강하게 유지할 수 있을 때 선택하세요.')
    ),
    '17.3',
    30,
    1
),
(
    'CHAMPION',
    'local-smoke-champion',
    '아리',
    '후방에서 안정적으로 피해를 누적하는 주문력 캐리입니다.',
    NULL,
    JSON_OBJECT(
        'cost', 4,
        'role', 'AP Carry',
        'position', 'Backline',
        'traits', JSON_ARRAY('전략가'),
        'bestItems', JSON_ARRAY('구인수의 격노검'),
        'stats', JSON_OBJECT('ad', 50, 'armor', 30, 'attackSpeed', 0.75, 'hp', 900, 'mana', 80, 'mr', 30, 'range', 4),
        'ability', JSON_OBJECT('name', '혼령 폭발', 'description', '가장 위협적인 적에게 주문 피해를 입힙니다.', 'iconUrl', NULL)
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
    focus,
    content,
    highlights_json,
    representative_image_url,
    published_at,
    is_current
) VALUES (
    '17.3',
    '17.3 패치노트',
    '17.3 패치의 주요 밸런스 변경사항입니다.',
    '챔피언 상향, 시너지 조정',
    '챔피언과 시너지 변경사항을 확인할 수 있는 기본 패치노트입니다.',
    JSON_ARRAY('초반 운영 선택지가 늘었습니다.', '중반 전환 구간의 안정성이 조정됐습니다.'),
    NULL,
    '2026-06-09 00:00:00',
    1
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    summary = VALUES(summary),
    focus = VALUES(focus),
    content = VALUES(content),
    highlights_json = VALUES(highlights_json),
    representative_image_url = VALUES(representative_image_url),
    published_at = VALUES(published_at),
    is_current = VALUES(is_current),
    deleted_at = NULL;

DELETE pc
FROM patch_note_changes pc
JOIN patch_notes pn ON pn.id = pc.patch_note_id
WHERE pn.version = '17.3'
  AND pc.target_key LIKE 'local-smoke-%';

INSERT INTO patch_note_changes (
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
    '아리',
    '주문 피해량이 올라 후방 캐리 성능이 개선됐습니다.',
    '피해량 100',
    '피해량 120',
    NULL,
    JSON_ARRAY('챔피언', '상향'),
    10
),
(
    (SELECT id FROM patch_notes WHERE version = '17.3'),
    'TRAIT',
    'ADJUST',
    'MEDIUM',
    'local-smoke-trait',
    '전략가',
    '활성 구간이 늘어나 중반 조합 전환이 쉬워졌습니다.',
    '2/4',
    '2/4/6',
    NULL,
    JSON_ARRAY('시너지', '조정'),
    20
);

COMMIT;
