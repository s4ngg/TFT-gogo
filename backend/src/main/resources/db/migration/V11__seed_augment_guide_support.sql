-- QA seed for guide augment selection plans.
-- Manual SQL until migration tooling is introduced.
-- Safe to re-run: the local smoke patch_version rows are replaced.

SET NAMES utf8mb4;

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
