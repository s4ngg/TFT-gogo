-- PR #207: ensure only one active, non-deleted patch note can be marked current.
-- Manual apply is required because local JPA ddl-auto is none.
-- MySQL unique indexes allow multiple NULL values, so only rows matching the
-- current/active/not-deleted condition are constrained to a single value.

CREATE UNIQUE INDEX uk_patch_notes_single_current_active
    ON patch_notes (
        (CASE
            WHEN is_current = 1 AND is_active = 1 AND deleted_at IS NULL THEN 1
            ELSE NULL
        END)
    );
