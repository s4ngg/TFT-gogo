-- Local smoke schema for the QA bootstrap database.
-- Schema management is manual in this project because JPA ddl-auto is none
-- and no Flyway/Liquibase dependency is configured.
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
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY ux_users_social_provider_social_id (social_provider, social_id),
    CONSTRAINT chk_users_social_fields_together
        CHECK (
            (social_provider IS NULL AND social_id IS NULL)
            OR (social_provider IS NOT NULL AND social_id IS NOT NULL)
        )
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
    tone VARCHAR(255) NULL,
    icon_url VARCHAR(255) NULL,
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
    is_recommended TINYINT(1) NOT NULL DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS guides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guide_type VARCHAR(20) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    summary TEXT NULL,
    image_url VARCHAR(500) NULL,
    data_json JSON NOT NULL,
    patch_version VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guides_type_target_patch (guide_type, target_key, patch_version),
    KEY idx_guides_public_patch (patch_version, is_active, deleted_at, sort_order, id),
    KEY idx_guides_public_type_patch (guide_type, patch_version, is_active, deleted_at, sort_order, id),
    KEY idx_guides_admin (patch_version, guide_type, is_active, deleted_at, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NULL,
    focus VARCHAR(200) NULL,
    content MEDIUMTEXT NOT NULL,
    highlights_json JSON NULL,
    representative_image_url VARCHAR(500) NULL,
    source_key VARCHAR(150) NULL,
    source_url VARCHAR(500) NULL,
    import_source VARCHAR(30) NULL,
    source_locale VARCHAR(20) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    imported_at DATETIME(6) NULL,
    published_at DATETIME(6) NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patch_notes_version (version),
    UNIQUE KEY uk_patch_notes_source_key (source_key),
    UNIQUE KEY uk_patch_notes_source_url (source_url),
    KEY idx_patch_notes_public (deleted_at, is_current, published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS patch_note_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_note_id BIGINT NOT NULL,
    source_key VARCHAR(150) NULL,
    source_heading_path VARCHAR(500) NULL,
    source_order INT NULL,
    imported_at DATETIME(6) NULL,
    manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    category VARCHAR(20) NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    impact VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
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
    CONSTRAINT fk_patch_note_changes_patch_note
        FOREIGN KEY (patch_note_id) REFERENCES patch_notes (id) ON DELETE RESTRICT,
    UNIQUE KEY uk_patch_note_changes_source_key (patch_note_id, source_key),
    KEY idx_patch_note_changes_public (patch_note_id, sort_order, id),
    KEY idx_patch_note_changes_source_order (patch_note_id, source_order),
    KEY idx_patch_note_changes_filters (patch_note_id, category, change_type, impact)
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
    tag VARCHAR(30) NOT NULL,
    PRIMARY KEY (party_post_id, tag_order),
    CONSTRAINT fk_party_post_tags_post
        FOREIGN KEY (party_post_id) REFERENCES party_posts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ELD reserved tables. Current MVP chat runtime uses CommunityChatRoomIds and
-- InMemoryChatServiceImpl, so these tables are not read or written by the app yet.
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

-- ELD reserved table. Current MVP chat messages are in-memory only.
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
