-- QA seed for guide augment support panels.
-- Manual SQL until migration tooling is introduced.
-- Safe to re-run: the local smoke patch_version rows are replaced.

SET NAMES utf8mb4;

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
