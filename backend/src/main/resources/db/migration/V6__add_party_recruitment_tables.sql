-- Issue #258: 파티 모집글 API 추가
-- Shared ERD snapshot 기준 party_posts, party_applications 테이블을 준비한다.
-- ddl-auto=none 환경에서 수동 적용 필요
-- party_post_tags는 ERD에 없는 커스텀 태그 저장용 보조 테이블이다.

CREATE TABLE IF NOT EXISTS party_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '작성자 ID (FK)',
    title VARCHAR(200) NOT NULL COMMENT '모집 글 제목',
    content TEXT NOT NULL COMMENT '모집 글 본문',
    max_members INT NOT NULL COMMENT '최대 모집 인원',
    current_members INT NOT NULL COMMENT '현재 참가 인원 (작성자 포함)',
    deadline DATETIME(6) NULL COMMENT '모집 마감 일시',
    is_closed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '모집 마감 여부 (1:마감, 0:모집중)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '최종 수정일시',
    game_mode VARCHAR(30) NULL COMMENT '게임 모드 (RANKED_TFT, NORMAL_TFT 등)',
    deleted_at DATETIME(6) NULL COMMENT '삭제일시 (Soft Delete)',
    PRIMARY KEY (id),
    KEY idx_party_posts_list (deleted_at, game_mode, is_closed, created_at, id),
    KEY idx_party_posts_user (user_id),
    CONSTRAINT fk_party_posts_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    party_post_id BIGINT NOT NULL COMMENT '모집 글 ID (FK)',
    user_id BIGINT NOT NULL COMMENT '신청자 ID (FK)',
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL COMMENT '신청 상태',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '신청일시',
    message TEXT NULL COMMENT '신청 시 한마디',
    responded_at DATETIME(6) NULL COMMENT '수락/거절 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_applications_post_user (party_post_id, user_id),
    KEY idx_party_applications_post_user_status (party_post_id, user_id, status),
    KEY idx_party_applications_user (user_id),
    CONSTRAINT fk_party_applications_post FOREIGN KEY (party_post_id) REFERENCES party_posts (id),
    CONSTRAINT fk_party_applications_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_post_tags (
    party_post_id BIGINT NOT NULL,
    tag_order INT NOT NULL,
    tag VARCHAR(30) NOT NULL,
    PRIMARY KEY (party_post_id, tag_order),
    CONSTRAINT fk_party_post_tags_post FOREIGN KEY (party_post_id) REFERENCES party_posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
