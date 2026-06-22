-- ERD 최종본(0621) 기준 덱 도메인 스키마 동기화
-- 이슈 #422: meta_decks.rank_filter 누락 및 덱 관련 컬럼 불일치 해소

-- 1. meta_decks: rank_filter 누락 대응 (ERDCloud 구버전으로 셋업한 경우)
ALTER TABLE meta_decks
    ADD COLUMN IF NOT EXISTS rank_filter VARCHAR(20) NOT NULL DEFAULT 'MASTER_PLUS'
    COMMENT '랭크 필터';

-- 2. deck_traits: 컬럼 길이를 ERD 최종본에 맞게 수정
--    tone: VARCHAR(255) → VARCHAR(30)
--    icon_url: VARCHAR(255) → VARCHAR(500)
ALTER TABLE deck_traits
    MODIFY COLUMN tone     VARCHAR(30)  NULL    COMMENT '색상 톤',
    MODIFY COLUMN icon_url VARCHAR(500) NULL    COMMENT '특성 아이콘 URL';

-- 3. hero_augments: is_recommended 기본값 0 → 1 (ERD 최종본)
ALTER TABLE hero_augments
    ALTER COLUMN is_recommended SET DEFAULT 1;
