CREATE TABLE IF NOT EXISTS tft_guide_champions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    champion_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    cost TINYINT NOT NULL,
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
