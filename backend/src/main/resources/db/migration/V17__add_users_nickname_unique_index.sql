UPDATE users duplicate_user
JOIN (
    SELECT candidate_user.user_id
    FROM users candidate_user
    JOIN users earlier_user
        ON earlier_user.nickname = candidate_user.nickname
        AND earlier_user.user_id < candidate_user.user_id
    GROUP BY candidate_user.user_id
) duplicate_rows
    ON duplicate_rows.user_id = duplicate_user.user_id
SET duplicate_user.nickname = CONCAT(
    LEFT(duplicate_user.nickname, GREATEST(1, 50 - CHAR_LENGTH(CONCAT('-u', duplicate_user.user_id)))),
    '-u',
    duplicate_user.user_id
);

-- Abort before adding the unique constraint if deterministic renaming still left duplicates.
CREATE TEMPORARY TABLE nickname_unique_guard (
    duplicate_marker INT NOT NULL
);

INSERT INTO nickname_unique_guard (duplicate_marker)
SELECT NULL
FROM users
GROUP BY nickname
HAVING COUNT(*) > 1
LIMIT 1;

DROP TEMPORARY TABLE nickname_unique_guard;

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);
