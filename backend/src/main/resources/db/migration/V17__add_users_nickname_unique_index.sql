CREATE TEMPORARY TABLE nickname_duplicate_users AS
SELECT candidate_user.user_id
FROM users candidate_user
JOIN users earlier_user
    ON earlier_user.nickname = candidate_user.nickname
    AND earlier_user.user_id < candidate_user.user_id
GROUP BY candidate_user.user_id;

CREATE TEMPORARY TABLE nickname_dedup_sequence AS
SELECT ones.n + tens.n * 10 + hundreds.n * 100 AS n
FROM (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds;

CREATE TEMPORARY TABLE nickname_dedup_targets (
    user_id BIGINT NOT NULL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_nickname_dedup_targets_nickname (nickname)
);

INSERT INTO nickname_dedup_targets (user_id, nickname)
SELECT duplicate_user.user_id,
       CONCAT('__tftgogo_dedup_', duplicate_user.user_id, '_', MIN(seq.n))
FROM users duplicate_user
JOIN nickname_duplicate_users
    ON nickname_duplicate_users.user_id = duplicate_user.user_id
CROSS JOIN nickname_dedup_sequence seq
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing_user
    WHERE existing_user.nickname = CONCAT('__tftgogo_dedup_', duplicate_user.user_id, '_', seq.n)
)
GROUP BY duplicate_user.user_id;

CREATE TEMPORARY TABLE nickname_unique_guard (
    duplicate_marker INT NOT NULL
);

INSERT INTO nickname_unique_guard (duplicate_marker)
SELECT NULL
FROM nickname_duplicate_users
LEFT JOIN nickname_dedup_targets
    ON nickname_dedup_targets.user_id = nickname_duplicate_users.user_id
WHERE nickname_dedup_targets.user_id IS NULL
LIMIT 1;

UPDATE users duplicate_user
JOIN nickname_dedup_targets
    ON nickname_dedup_targets.user_id = duplicate_user.user_id
SET duplicate_user.nickname = nickname_dedup_targets.nickname;

INSERT INTO nickname_unique_guard (duplicate_marker)
SELECT NULL
FROM users
GROUP BY nickname
HAVING COUNT(*) > 1
LIMIT 1;

DROP TEMPORARY TABLE nickname_unique_guard;
DROP TEMPORARY TABLE nickname_dedup_targets;
DROP TEMPORARY TABLE nickname_dedup_sequence;
DROP TEMPORARY TABLE nickname_duplicate_users;

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
