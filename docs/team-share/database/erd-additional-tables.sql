-- ============================================================
-- TFT-gogo  추가 테이블 DDL
-- 기존 ERD (14개 테이블)에 추가로 필요한 테이블 정의
-- ERD Cloud → SQL 가져오기 탭에 붙여넣기하여 사용
-- ============================================================


-- ──────────────────────────────────────────────────────────
-- 1. refresh_tokens  (JWT 리프레시 토큰 관리)
-- ──────────────────────────────────────────────────────────
-- users 테이블의 id 타입(CHAR 36 UUID)에 맞춰 user_id 정의
CREATE TABLE refresh_tokens (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id     CHAR(36)        NOT NULL COMMENT 'users.id (UUID)',
    token       VARCHAR(512)    NOT NULL COMMENT 'Refresh Token 값 (해시 저장 권장)',
    expires_at  DATETIME        NOT NULL COMMENT '만료 일시',
    revoked_at  DATETIME        NULL     COMMENT '폐기 일시 (로그아웃/재발급 시)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rt_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_rt_token   (token(64)),
    INDEX idx_rt_user_id (user_id)
) COMMENT='JWT 리프레시 토큰 – 로그아웃·재발급 시 revoked_at 기록';


-- ──────────────────────────────────────────────────────────
-- 2. patch_notes  (패치노트 페이지)
-- ──────────────────────────────────────────────────────────
CREATE TABLE patch_notes (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    version         VARCHAR(20)     NOT NULL COMMENT '패치 버전 ex) 14.5',
    title           VARCHAR(200)    NOT NULL,
    content         MEDIUMTEXT      NOT NULL COMMENT 'HTML 또는 Markdown',
    summary         TEXT            NULL     COMMENT '한 줄 요약 (카드 미리보기용)',
    published_at    DATETIME        NOT NULL COMMENT 'Riot 공식 발행 일시',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_pn_version (version),
    INDEX idx_pn_published (published_at)
) COMMENT='TFT 패치노트 – Riot 공식 패치노트를 크롤링/수동 등록';


-- ──────────────────────────────────────────────────────────
-- 3. guides  (가이드 페이지)
-- ──────────────────────────────────────────────────────────
CREATE TABLE guides (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    author_id   CHAR(36)        NOT NULL COMMENT 'users.id',
    title       VARCHAR(200)    NOT NULL,
    content     MEDIUMTEXT      NOT NULL,
    category    ENUM(
                    'BEGINNER',     -- 입문
                    'INTERMEDIATE', -- 중급
                    'ADVANCED',     -- 고급
                    'MECHANICS'     -- 게임 메카닉
                )               NOT NULL DEFAULT 'BEGINNER',
    view_count  INT             NOT NULL DEFAULT 0,
    is_pinned   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '관리자 고정 여부',
    deleted_at  DATETIME        NULL     COMMENT 'Soft Delete',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_guides_author
        FOREIGN KEY (author_id) REFERENCES users(id),
    INDEX idx_guides_category (category),
    INDEX idx_guides_created  (created_at)
) COMMENT='공략 가이드 – 유저 또는 운영자가 작성하는 TFT 전략 문서';


-- ──────────────────────────────────────────────────────────
-- 4. meta_decks  (덱 모음 · 메타통계 페이지)
-- ──────────────────────────────────────────────────────────
CREATE TABLE meta_decks (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL COMMENT '덱 이름 ex) 원소술사 징크스',
    patch_version   VARCHAR(20)     NOT NULL COMMENT '기준 패치 버전',
    tier            ENUM('S','A','B','C') NOT NULL DEFAULT 'B',
    play_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '픽률 (%)',
    win_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '1등률 (%)',
    top4_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT 'Top4 확률 (%)',
    avg_placement   DECIMAL(4,2)    NOT NULL DEFAULT 0.00 COMMENT '평균 등수',
    sample_size     INT             NOT NULL DEFAULT 0    COMMENT '집계 게임 수',
    description     TEXT            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_md_patch (patch_version),
    INDEX idx_md_tier  (tier)
) COMMENT='메타 덱 정보 – 패치별 티어·통계 집계 결과';


-- ──────────────────────────────────────────────────────────
-- 5. deck_traits  (덱 특성/시너지)
-- ──────────────────────────────────────────────────────────
CREATE TABLE deck_traits (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    deck_id     BIGINT          NOT NULL,
    trait_id    VARCHAR(60)     NOT NULL COMMENT 'CDragon trait ID ex) TFT12_Arcana',
    trait_name  VARCHAR(100)    NOT NULL COMMENT '표시 이름 ex) 비전술사',
    num_units   INT             NOT NULL COMMENT '해당 특성 보유 유닛 수',

    CONSTRAINT fk_dt_deck
        FOREIGN KEY (deck_id) REFERENCES meta_decks(id) ON DELETE CASCADE,
    INDEX idx_dt_deck_id (deck_id)
) COMMENT='덱별 특성(시너지) 구성 – CDragon trait ID 기준';


-- ──────────────────────────────────────────────────────────
-- 6. deck_units  (덱 챔피언 구성)
-- ──────────────────────────────────────────────────────────
CREATE TABLE deck_units (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    deck_id             BIGINT          NOT NULL,
    character_id        VARCHAR(60)     NOT NULL COMMENT 'CDragon unit ID ex) TFT12_Jinx',
    champion_name       VARCHAR(100)    NOT NULL COMMENT '표시 이름 ex) 징크스',
    cost                INT             NOT NULL COMMENT '코스트 1~5',
    is_carry            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '캐리 챔피언 여부',
    recommended_items   JSON            NULL     COMMENT '추천 아이템 ID 배열',
    star_level          INT             NOT NULL DEFAULT 2 COMMENT '목표 별 수 (1~3)',

    CONSTRAINT fk_du_deck
        FOREIGN KEY (deck_id) REFERENCES meta_decks(id) ON DELETE CASCADE,
    INDEX idx_du_deck_id (deck_id)
) COMMENT='덱별 챔피언 구성 – CDragon character ID 기준';


-- ──────────────────────────────────────────────────────────
-- 7. hero_augments  (영웅 증강 추천·비추천)
-- ──────────────────────────────────────────────────────────
-- 덱의 특정 챔피언에 붙는 영웅 증강 효율 데이터
CREATE TABLE hero_augments (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    deck_id         BIGINT          NOT NULL,
    character_id    VARCHAR(60)     NOT NULL COMMENT '대상 챔피언 CDragon ID',
    augment_id      VARCHAR(100)    NOT NULL COMMENT 'CDragon 영웅 증강 ID',
    augment_name    VARCHAR(200)    NOT NULL,
    is_recommended  TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '1=추천 / 0=비추천',
    win_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    top4_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    avg_placement   DECIMAL(4,2)    NOT NULL DEFAULT 0.00,
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '정렬 순서 (낮을수록 추천)',

    CONSTRAINT fk_ha_deck
        FOREIGN KEY (deck_id) REFERENCES meta_decks(id) ON DELETE CASCADE,
    INDEX idx_ha_deck_id    (deck_id),
    INDEX idx_ha_character  (character_id)
) COMMENT='영웅 증강 추천/비추천 – 왼쪽=추천(sort_order 오름차순)';


-- ──────────────────────────────────────────────────────────
-- 8. artifact_stats  (유물 통계 – 덱 페이지 하단)
-- ──────────────────────────────────────────────────────────
CREATE TABLE artifact_stats (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    patch_version   VARCHAR(20)     NOT NULL,
    deck_id         BIGINT          NULL     COMMENT 'NULL = 전체 통계 / NOT NULL = 덱별 통계',
    item_id         VARCHAR(100)    NOT NULL COMMENT 'CDragon item ID',
    item_name       VARCHAR(200)    NOT NULL,
    play_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '빈도수 (%)',
    win_rate        DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '승률 (%)',
    top4_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT 'Top4 확률 (%)',
    avg_placement   DECIMAL(4,2)    NOT NULL DEFAULT 0.00 COMMENT '평균 등수',
    placement_delta DECIMAL(4,2)    NOT NULL DEFAULT 0.00 COMMENT '평균 대비 등수 변화 (음수=좋음)',
    sample_size     INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_as_deck
        FOREIGN KEY (deck_id) REFERENCES meta_decks(id) ON DELETE SET NULL,
    INDEX idx_as_patch   (patch_version),
    UNIQUE KEY uq_as_patch_deck_item (patch_version, deck_id, item_id)
) COMMENT='유물(아이템) 통계 – 덱별 또는 전체 기준 빈도·승률 집계';


-- ──────────────────────────────────────────────────────────
-- 9. ai_recommendations  (AI 추천 페이지 캐싱)
-- ──────────────────────────────────────────────────────────
CREATE TABLE ai_recommendations (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         CHAR(36)        NULL     COMMENT '로그인 유저 (비로그인 NULL)',
    summoner_puuid  VARCHAR(78)     NOT NULL COMMENT 'Riot PUUID',
    request_hash    CHAR(64)        NOT NULL COMMENT 'SHA-256(요청 파라미터) – 캐시 키',
    recommendation  MEDIUMTEXT      NOT NULL COMMENT 'AI 응답 JSON (덱·운영·증강 추천)',
    model_version   VARCHAR(50)     NOT NULL DEFAULT 'gpt-4o-mini',
    expires_at      DATETIME        NOT NULL COMMENT '캐시 만료 일시',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_air_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uq_air_hash      (request_hash),
    INDEX      idx_air_puuid   (summoner_puuid),
    INDEX      idx_air_expires (expires_at)
) COMMENT='AI 추천 결과 캐시 – 동일 요청 재호출 방지 및 OpenAI 비용 절감';
