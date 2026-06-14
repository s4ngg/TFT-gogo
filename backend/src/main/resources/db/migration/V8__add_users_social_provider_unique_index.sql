CREATE UNIQUE INDEX ux_users_social_provider_social_id
    ON users (social_provider, social_id);
