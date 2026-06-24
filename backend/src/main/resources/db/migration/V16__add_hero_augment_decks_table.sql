CREATE TABLE hero_augment_decks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    champions   TEXT,
    traits      TEXT,
    board_positions TEXT,
    hero_augments   TEXT,
    recommended BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    grade       VARCHAR(10),
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL
);
