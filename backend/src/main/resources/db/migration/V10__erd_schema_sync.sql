-- ERD 최종본(0621) 기준 스키마 동기화
-- 이슈 #422: meta_decks.rank_filter 누락 및 컬럼 불일치 해소

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

-- 4. chat_room_members 테이블 신규 추가 (ERD 최종본 포함, 🔵 구현 예정)
CREATE TABLE IF NOT EXISTS chat_room_members (
    id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '채팅방 멤버 ID',
    chat_room_id BIGINT       NOT NULL                 COMMENT '채팅방 ID',
    user_id      BIGINT       NOT NULL                 COMMENT '회원 ID',
    joined_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)  COMMENT '입장 일시',
    left_at      DATETIME(6)  NULL                     COMMENT '퇴장 일시',
    PRIMARY KEY (id),
    KEY idx_chat_room_members_room (chat_room_id, joined_at),
    KEY idx_chat_room_members_user (user_id),
    CONSTRAINT fk_chat_room_members_room
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_room_members_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '채팅방 멤버 (접속자 관리)';
