CREATE UNIQUE INDEX ux_users_social_provider_social_id
    ON users (social_provider, social_id);

ALTER TABLE users ADD CONSTRAINT chk_users_social_fields_together
    CHECK (
        (social_provider IS NULL AND social_id IS NULL)
        OR (social_provider IS NOT NULL AND social_id IS NOT NULL)
    );
