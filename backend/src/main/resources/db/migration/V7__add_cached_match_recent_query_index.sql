-- PR #352: optimize recent cached match lookups for guide metric statistics.
-- Manual apply is required because local JPA ddl-auto is none.

CREATE INDEX idx_cached_match_queue_datetime_match
    ON cached_match (queue_id, game_datetime DESC, match_id DESC);
