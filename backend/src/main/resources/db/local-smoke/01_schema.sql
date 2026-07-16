-- Human-readable reference of the current schema state.
-- NOT mounted by docker-compose; schema is created by Flyway V1__init_schema.sql.
-- Keep column names aligned with the current JPA physical naming strategy.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image VARCHAR(500) NULL,
    social_provider VARCHAR(20) NULL,
    social_id VARCHAR(255) NULL,
    notification_enabled TINYINT(1) NOT NULL DEFAULT 1,
    auth_token_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_nickname (nickname),
    UNIQUE KEY ux_users_social_provider_social_id (social_provider, social_id),
    CONSTRAINT chk_users_social_fields_together
        CHECK (
            (social_provider IS NULL AND social_id IS NULL)
            OR (social_provider IS NOT NULL AND social_id IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS cached_summoner (
    puuid VARCHAR(100) NOT NULL,
    game_name VARCHAR(100) NOT NULL,
    tag_line VARCHAR(50) NOT NULL,
    profile_icon_id INT NOT NULL,
    summoner_level BIGINT NOT NULL,
    cached_at DATETIME(6) NOT NULL,
    PRIMARY KEY (puuid),
    KEY idx_cached_summoner_name_tag (game_name, tag_line)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cached_rank (
    puuid VARCHAR(100) NOT NULL,
    tier VARCHAR(20) NULL,
    rank_value VARCHAR(10) NULL,
    league_points INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    cached_at DATETIME(6) NOT NULL,
    PRIMARY KEY (puuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cached_match (
    match_id VARCHAR(50) NOT NULL,
    queue_id INT NOT NULL,
    game_datetime BIGINT NOT NULL,
    match_json MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (match_id),
    KEY idx_cached_match_queue_datetime_match (queue_id, game_datetime DESC, match_id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cached_match_participant (
    match_id VARCHAR(50) NOT NULL,
    puuid VARCHAR(100) NOT NULL,
    PRIMARY KEY (match_id, puuid),
    KEY idx_cached_match_participant_puuid_match (puuid, match_id),
    CONSTRAINT fk_cached_match_participant_match
        FOREIGN KEY (match_id) REFERENCES cached_match (match_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meta_decks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    signature VARCHAR(255) NOT NULL,
    rank_filter VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    tier VARCHAR(5) NOT NULL,
    play_rate DOUBLE NOT NULL DEFAULT 0,
    win_rate DOUBLE NOT NULL DEFAULT 0,
    top4_rate DOUBLE NOT NULL DEFAULT 0,
    avg_placement DOUBLE NOT NULL DEFAULT 0,
    sample_size INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    data_start_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_meta_decks_signature_rank_patch (signature, rank_filter, patch_version),
    KEY idx_meta_decks_rank_patch_play_rate (rank_filter, patch_version, play_rate),
    KEY idx_meta_decks_rank_data_start (rank_filter, data_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deck_units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meta_decks_id BIGINT NOT NULL,
    character_id VARCHAR(60) NOT NULL,
    champion_name VARCHAR(100) NOT NULL,
    cost INT NOT NULL,
    is_carry TINYINT(1) NOT NULL DEFAULT 0,
    recommended_items JSON NULL,
    star_level INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_deck_units_meta_decks_id (meta_decks_id),
    CONSTRAINT fk_deck_units_meta_deck
        FOREIGN KEY (meta_decks_id) REFERENCES meta_decks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deck_traits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meta_decks_id BIGINT NOT NULL,
    trait_id VARCHAR(60) NOT NULL,
    trait_name VARCHAR(100) NOT NULL,
    num_units INT NOT NULL,
    tone VARCHAR(30) NULL,
    icon_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_deck_traits_meta_decks_id (meta_decks_id),
    CONSTRAINT fk_deck_traits_meta_deck
        FOREIGN KEY (meta_decks_id) REFERENCES meta_decks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hero_augments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meta_decks_id BIGINT NOT NULL,
    character_id VARCHAR(60) NOT NULL,
    augment_id VARCHAR(100) NOT NULL,
    augment_name VARCHAR(200) NOT NULL,
    is_recommended TINYINT(1) NOT NULL DEFAULT 1,
    win_rate DOUBLE NOT NULL DEFAULT 0,
    top4_rate DOUBLE NOT NULL DEFAULT 0,
    avg_placement DOUBLE NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hero_augments_meta_decks_id (meta_decks_id),
    CONSTRAINT fk_hero_augments_meta_deck
        FOREIGN KEY (meta_decks_id) REFERENCES meta_decks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS artifact_stats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meta_decks_id BIGINT NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    item_id VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    play_rate DOUBLE NOT NULL DEFAULT 0,
    win_rate DOUBLE NOT NULL DEFAULT 0,
    top4_rate DOUBLE NOT NULL DEFAULT 0,
    avg_placement DOUBLE NOT NULL DEFAULT 0,
    placement_delta DOUBLE NOT NULL DEFAULT 0,
    sample_size INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_artifact_stats_meta_decks_id (meta_decks_id),
    KEY idx_artifact_stats_patch_item (patch_version, item_id),
    CONSTRAINT fk_artifact_stats_meta_deck
        FOREIGN KEY (meta_decks_id) REFERENCES meta_decks (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deck_curations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    signature VARCHAR(255) NOT NULL,
    rank_filter VARCHAR(20) NOT NULL,
    custom_name VARCHAR(200) NULL,
    is_hidden TINYINT(1) NOT NULL DEFAULT 0,
    sort_priority INT NULL,
    curator_note TEXT NULL,
    board_positions TEXT NULL,
    play_guide TEXT NULL,
    hero_augments TEXT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_deck_curations_signature_rank (signature, rank_filter),
    KEY idx_deck_curations_rank_filter (rank_filter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tft_guide_champions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    champion_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    cost INT NOT NULL,
    role VARCHAR(50) NOT NULL,
    position VARCHAR(50) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    stats_json JSON NOT NULL,
    traits_json JSON NOT NULL,
    best_items_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tft_guide_champions_key_patch (champion_key, patch_version),
    KEY idx_tft_guide_champions_patch_cost (patch_version, cost, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tft_guide_traits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trait_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    tone VARCHAR(30) NOT NULL,
    summary TEXT NOT NULL,
    levels_json JSON NOT NULL,
    tier_effects_json JSON NOT NULL,
    champions_json JSON NOT NULL,
    special_units_json JSON NOT NULL,
    tips_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tft_guide_traits_key_patch (trait_key, patch_version),
    KEY idx_tft_guide_traits_patch (patch_version, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tft_guide_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    description TEXT NULL,
    stats_json JSON NOT NULL,
    best_users_json JSON NOT NULL,
    combinations_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tft_guide_items_key_patch (item_key, patch_version),
    KEY idx_tft_guide_items_patch (patch_version, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tft_guide_augments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    augment_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon_url VARCHAR(500) NULL,
    tags_json JSON NOT NULL,
    stats_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tft_guide_augments_key_patch (augment_key, patch_version),
    KEY idx_tft_guide_augments_patch_name (patch_version, name, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tft_guide_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_version VARCHAR(20) NOT NULL,
    source_set_number INT UNSIGNED NULL,
    source_mutator VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    champion_count INT UNSIGNED NOT NULL DEFAULT 0,
    trait_count INT UNSIGNED NOT NULL DEFAULT 0,
    item_count INT UNSIGNED NOT NULL DEFAULT 0,
    augment_count INT UNSIGNED NOT NULL DEFAULT 0,
    validated_at DATETIME(6) NULL,
    activated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tft_guide_snapshots_patch (patch_version),
    KEY idx_tft_guide_snapshots_status_activated (status, activated_at, id),
    CONSTRAINT chk_tft_guide_snapshots_status
        CHECK (status IN ('STAGING', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_tft_guide_snapshots_active_validation
        CHECK (
            (validated_at IS NULL AND status <> 'ACTIVE')
            OR (
                validated_at IS NOT NULL
                AND champion_count > 0
                AND trait_count > 0
                AND item_count > 0
                AND augment_count > 0
            )
        ),
    CONSTRAINT chk_tft_guide_snapshots_source_pair
        CHECK (
            (source_set_number IS NULL AND source_mutator IS NULL)
            OR (
                source_set_number IS NOT NULL
                AND source_set_number > 0
                AND source_mutator IS NOT NULL
                AND CHAR_LENGTH(TRIM(source_mutator)) > 0
            )
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uk_tft_guide_snapshots_single_active
    ON tft_guide_snapshots (
        (CASE
            WHEN status = 'ACTIVE' THEN 1
            ELSE NULL
        END)
    );

CREATE TABLE IF NOT EXISTS patch_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NULL,
    content MEDIUMTEXT NOT NULL,
    focus VARCHAR(200) NULL,
    representative_image_url VARCHAR(500) NULL,
    source_key VARCHAR(150) NULL,
    source_url VARCHAR(500) NULL,
    import_source VARCHAR(30) NULL,
    source_locale VARCHAR(20) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    imported_at DATETIME(6) NULL,
    published_at DATETIME(6) NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    highlights_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_notes_version (version),
    UNIQUE KEY uk_patch_notes_source_key (source_key),
    UNIQUE KEY uk_patch_notes_source_url (source_url),
    KEY idx_patch_notes_public (deleted_at, is_current, published_at, id),
    KEY idx_patch_notes_history (deleted_at, published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uk_patch_notes_single_current
    ON patch_notes (
        (CASE
            WHEN is_current = 1 AND deleted_at IS NULL THEN 1
            ELSE NULL
        END)
    );

CREATE TABLE IF NOT EXISTS patch_note_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    source_key VARCHAR(150) NULL,
    source_heading_path VARCHAR(500) NULL,
    source_order INT NULL,
    imported_at DATETIME(6) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    category ENUM('CHAMPION','TRAIT','ITEM','AUGMENT','SYSTEM') NOT NULL,
    change_type ENUM('BUFF','NERF','ADJUST','NEW') NOT NULL,
    impact ENUM('HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
    target_key VARCHAR(100) NULL,
    target_name VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    image_url VARCHAR(500) NULL,
    tags_json JSON NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_note_changes_source_key (patch_note_id, source_key),
    KEY idx_patch_note_changes_source_order (patch_note_id, source_order),
    KEY idx_patch_note_changes_public (patch_note_id, sort_order, id),
    KEY idx_patch_note_changes_filters (patch_note_id, category, change_type, impact),
    CONSTRAINT fk_patch_note_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    game_mode VARCHAR(30) NULL,
    max_members INT NOT NULL,
    current_members INT NOT NULL,
    deadline DATETIME(6) NULL,
    is_closed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT chk_party_posts_max_members_positive
        CHECK (max_members > 0),
    CONSTRAINT chk_party_posts_current_members_bounds
        CHECK (current_members >= 0 AND current_members <= max_members),
    PRIMARY KEY (id),
    KEY idx_party_posts_list (deleted_at, game_mode, is_closed, created_at, id),
    KEY idx_party_posts_user (user_id),
    CONSTRAINT fk_party_posts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    party_post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    message TEXT NULL,
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_applications_post_user (party_post_id, user_id),
    KEY idx_party_applications_post_user_status (party_post_id, user_id, status),
    KEY idx_party_applications_user (user_id),
    CONSTRAINT fk_party_applications_post
        FOREIGN KEY (party_post_id) REFERENCES party_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_party_applications_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS party_post_tags (
    party_post_id BIGINT NOT NULL,
    tag_order INT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (party_post_id, tag_order),
    CONSTRAINT fk_party_post_tags_post
        FOREIGN KEY (party_post_id) REFERENCES party_posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Runtime chat room metadata. The local profile may still use InMemoryChatServiceImpl,
-- but non-local profiles persist recent chat messages through these tables.
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

-- Persistent chat messages used by the non-local ChatService implementation.
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

CREATE TABLE IF NOT EXISTS hero_augment_decks (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    champions       TEXT,
    traits          TEXT,
    board_positions TEXT,
    hero_augments   TEXT,
    recommended     TINYINT(1) NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0,
    grade           VARCHAR(10),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hero_augment_decks_sort (sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_accounts (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_accounts_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_refresh_tokens (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    admin_account_id BIGINT       NOT NULL,
    token_hash       VARCHAR(255) NOT NULL,
    expires_at       DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_refresh_tokens_hash (token_hash),
    KEY idx_admin_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_art_admin FOREIGN KEY (admin_account_id) REFERENCES admin_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    admin_id   BIGINT       NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    ip         VARCHAR(45)  NOT NULL,
    user_agent VARCHAR(500),
    action     VARCHAR(100) NOT NULL,
    target     VARCHAR(255),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_admin_audit_logs_admin_id (admin_id),
    KEY idx_admin_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
