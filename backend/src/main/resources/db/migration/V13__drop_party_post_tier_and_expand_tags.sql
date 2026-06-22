-- Issue #459 follow-up: 최종 ERD 계약에 맞춰 party_posts.tier를 제거하고 태그 길이를 확장한다.
-- V12는 이미 적용된 환경의 Flyway 이력을 보존하기 위해 삭제하지 않는다.

ALTER TABLE party_posts
    DROP COLUMN tier;

ALTER TABLE party_post_tags
    MODIFY COLUMN tag VARCHAR(50) NOT NULL;
