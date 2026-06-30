-- Issues #583, #585: refresh token rotation, access token blocklist, persistent chat runtime

SET @has_auth_token_version := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'auth_token_version'
);
SET @add_auth_token_version := IF(
    @has_auth_token_version = 0,
    'ALTER TABLE users ADD COLUMN auth_token_version BIGINT NOT NULL DEFAULT 0 COMMENT ''Access Token 일괄 무효화 버전'' AFTER notification_enabled',
    'SELECT 1'
);
PREPARE add_auth_token_version_statement FROM @add_auth_token_version;
EXECUTE add_auth_token_version_statement;
DEALLOCATE PREPARE add_auth_token_version_statement;

SET @has_users_deleted_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'deleted_at'
);
SET @add_users_deleted_at := IF(
    @has_users_deleted_at = 0,
    'ALTER TABLE users ADD COLUMN deleted_at DATETIME(6) NULL COMMENT ''탈퇴/정지 등 세션 무효화 기준 일시'' AFTER updated_at',
    'SELECT 1'
);
PREPARE add_users_deleted_at_statement FROM @add_users_deleted_at;
EXECUTE add_users_deleted_at_statement;
DEALLOCATE PREPARE add_users_deleted_at_statement;

CREATE TABLE IF NOT EXISTS refresh_token_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    reuse_detected TINYINT(1) NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_sessions_token_hash (token_hash),
    KEY idx_refresh_token_sessions_user_active (user_id, revoked, expires_at),
    CONSTRAINT fk_refresh_token_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS access_token_blocklist (
    token_id VARCHAR(80) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (token_id),
    KEY idx_access_token_blocklist_expiry (expires_at),
    KEY idx_access_token_blocklist_user (user_id),
    CONSTRAINT fk_access_token_blocklist_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_key VARCHAR(80) NOT NULL,
    creator_id BIGINT NULL,
    party_post_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_rooms_room_key (room_key),
    KEY idx_chat_rooms_creator (creator_id),
    KEY idx_chat_rooms_party_post (party_post_id),
    CONSTRAINT fk_chat_rooms_creator
        FOREIGN KEY (creator_id) REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_rooms_party_post
        FOREIGN KEY (party_post_id) REFERENCES party_posts (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chat_room_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_filtered TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_chat_messages_room_created (chat_room_id, created_at, id),
    KEY idx_chat_messages_user (user_id),
    CONSTRAINT fk_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
