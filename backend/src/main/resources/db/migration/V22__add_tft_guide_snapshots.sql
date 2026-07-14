CREATE TABLE tft_guide_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patch_version VARCHAR(20) NOT NULL,
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
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TEMPORARY TABLE tmp_complete_guide_snapshots (
    patch_version VARCHAR(20) NOT NULL,
    champion_count INT UNSIGNED NOT NULL,
    trait_count INT UNSIGNED NOT NULL,
    item_count INT UNSIGNED NOT NULL,
    augment_count INT UNSIGNED NOT NULL,
    PRIMARY KEY (patch_version)
);

-- Backfill only patch versions that already contain all four guide data types.
INSERT INTO tmp_complete_guide_snapshots (
    patch_version,
    champion_count,
    trait_count,
    item_count,
    augment_count
)
SELECT
    champions.patch_version,
    champions.row_count,
    traits.row_count,
    items.row_count,
    augments.row_count
FROM (
    SELECT patch_version, COUNT(*) AS row_count
    FROM tft_guide_champions
    GROUP BY patch_version
) champions
INNER JOIN (
    SELECT patch_version, COUNT(*) AS row_count
    FROM tft_guide_traits
    GROUP BY patch_version
) traits ON traits.patch_version = champions.patch_version
INNER JOIN (
    SELECT patch_version, COUNT(*) AS row_count
    FROM tft_guide_items
    GROUP BY patch_version
) items ON items.patch_version = champions.patch_version
INNER JOIN (
    SELECT patch_version, COUNT(*) AS row_count
    FROM tft_guide_augments
    GROUP BY patch_version
) augments ON augments.patch_version = champions.patch_version
-- Legacy rows cannot prove their original import job, so only grandfather data that
-- satisfies the same configured per-type minimums used by the runtime importer.
WHERE champions.row_count >= GREATEST(1, ${guideMinimumChampionCount})
  AND traits.row_count >= GREATEST(1, ${guideMinimumTraitCount})
  AND items.row_count >= GREATEST(1, ${guideMinimumItemCount})
  AND augments.row_count >= GREATEST(1, ${guideMinimumAugmentCount});

-- Preserve public data by activating the latest complete historical patch only.
SET @latest_complete_guide_patch := (
    SELECT patch_version
    FROM tmp_complete_guide_snapshots
    ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
             CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
             patch_version DESC
    LIMIT 1
);

INSERT INTO tft_guide_snapshots (
    patch_version,
    status,
    champion_count,
    trait_count,
    item_count,
    augment_count,
    validated_at,
    activated_at
)
SELECT
    patch_version,
    CASE WHEN patch_version = @latest_complete_guide_patch THEN 'ACTIVE' ELSE 'INACTIVE' END,
    champion_count,
    trait_count,
    item_count,
    augment_count,
    CURRENT_TIMESTAMP(6),
    CASE WHEN patch_version = @latest_complete_guide_patch THEN CURRENT_TIMESTAMP(6) ELSE NULL END
FROM tmp_complete_guide_snapshots;

DROP TEMPORARY TABLE tmp_complete_guide_snapshots;

CREATE UNIQUE INDEX uk_tft_guide_snapshots_single_active
    ON tft_guide_snapshots (
        (CASE
            WHEN status = 'ACTIVE' THEN 1
            ELSE NULL
        END)
    );
