-- Issue #459: 파티 모집글 티어 조건 저장
-- ddl-auto=none 환경에서 기존 party_posts 테이블에 수동 적용 필요

ALTER TABLE party_posts
    ADD COLUMN tier VARCHAR(30) NOT NULL DEFAULT '제한 없음' COMMENT '파티 모집글 티어 조건' AFTER current_members;
