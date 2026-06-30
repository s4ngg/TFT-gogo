<spec domain="admin">

<purpose>
Admin pages manage TFTgogo curation data, imports, monitoring, and protected operations.
All admin backend endpoints are protected by X-Admin-Token.
</purpose>

<routes>
- /admin -> admin login.
- /admin/decks -> meta deck curation.
- /admin/hero-augments -> hero augment deck curation.
- /admin/guides -> game guide CDragon import.
- /admin/match-monitor -> match cache and Riot API rate-limit monitoring.
- /admin/patch-notes -> patch-note manual curation and Riot official import.
- /admin/members -> placeholder.
- /admin/community -> placeholder.
</routes>

<frontend-structure>
- frontend/src/layouts/AdminLayout.tsx
- frontend/src/components/admin/AdminSidebar.tsx
- frontend/src/pages/Admin/AdminLogin.tsx
- frontend/src/pages/Admin/AdminDecks.tsx
- frontend/src/pages/Admin/AdminHeroAugments.tsx
- frontend/src/pages/Admin/AdminGuides.tsx
- frontend/src/pages/Admin/AdminMatchMonitor.tsx
- frontend/src/pages/Admin/AdminMatchMonitor.module.css
- frontend/src/pages/Admin/AdminPatchNotes.tsx
- frontend/src/pages/Admin/AdminMembers.tsx
- frontend/src/pages/Admin/AdminCommunity.tsx
- frontend/src/pages/Admin/Admin.module.css
- frontend/src/pages/Admin/components/RateLimitGauge.tsx
- frontend/src/api/adminApi.ts
</frontend-structure>

<backend-structure>
- Match admin controller: backend/src/main/java/com/tftgogo/domain/match/controller/AdminMatchController.java
- Match admin Swagger docs: backend/src/main/java/com/tftgogo/domain/match/controller/docs/AdminMatchControllerDocs.java
- Match admin service: backend/src/main/java/com/tftgogo/domain/match/service/AdminMatchService.java
- Match admin implementation: backend/src/main/java/com/tftgogo/domain/match/service/impl/AdminMatchServiceImpl.java
- CacheStatsResponse: backend/src/main/java/com/tftgogo/domain/match/dto/response/CacheStatsResponse.java
- RateLimitStatsResponse: backend/src/main/java/com/tftgogo/domain/match/dto/response/RateLimitStatsResponse.java
</backend-structure>

<api>
<backend>
- GET /api/admin/decks
- POST /api/admin/decks/meta/aggregate
- PATCH /api/admin/decks/{deckId}
- DELETE /api/admin/decks/{deckId}/curation

- GET /api/admin/hero-augment-decks
- POST /api/admin/hero-augment-decks
- PUT /api/admin/hero-augment-decks/{id}
- DELETE /api/admin/hero-augment-decks/{id}

- POST /api/admin/guides/import/cdragon

- GET /api/admin/match/cache-stats
- GET /api/admin/match/rate-limit

- GET /api/admin/patch-notes
- POST /api/admin/patch-notes
- POST /api/admin/patch-notes/import/riot
- PATCH /api/admin/patch-notes/{patchNoteId}
- DELETE /api/admin/patch-notes/{patchNoteId}
- GET /api/admin/patch-notes/{patchNoteId}/changes
- POST /api/admin/patch-note-changes
- PATCH /api/admin/patch-note-changes/{changeId}
- DELETE /api/admin/patch-note-changes/{changeId}
</backend>
</api>

<auth>
- AdminTokenFilter protects /api/admin/**.
- Frontend stores the admin token in localStorage key tftgogo_admin_token.
- Admin API functions must include X-Admin-Token.
- Missing/invalid token should route the user back to /admin login.
- Do not log admin tokens.
</auth>

<business-rules>
- Admin APIs should follow the public API response and validation conventions.
- Soft delete is preferred where deletedAt exists.
- JSON fields such as dataJson, highlightsJson, tagsJson, and planJson must contain valid JSON before persistence.
- Only one active, non-deleted patch note should be current.
- Swagger annotations belong in XxxControllerDocs interfaces, not directly in controllers.
- Placeholder member/community pages should show a ready-state screen and must not call unfinished APIs.
- Rate-limit monitoring should read RiotRateLimiter state and refresh through TanStack Query.
- Match cache stats are aggregated from CachedMatch rows and only manual refresh is supported.
</business-rules>

<deck-curation>
- Supports custom deck names, exposure toggles, and sort priority.
- Supports champion board positions, play guides, and hero augment edits.
- Aggregation data is identified by deck signature from the top trait combination.
- If no custom placement is configured, public detail pages use CDragon automatic placement data.
</deck-curation>

<guide-curation>
- Admin game guide manual CRUD is not part of the current backend contract.
- Admin guide writes currently use only POST /api/admin/guides/import/cdragon.
- Admin guide import form defaults patchVersion to `latest`. The backend resolves `latest` from the current patch note first, then the latest non-deleted patch note.
- If no patch note exists, `latest` guide import fails. Local QA should import patch notes first or pass an explicit patchVersion.
- Admin guide import should clearly show an importing/loading state. If an estimated progress UI is used, label it as estimated because the current import API is synchronous and does not expose real server-side progress.
- CDragon import writes champion/trait/item/augment guide data into split guide tables:
  - tft_guide_champions
  - tft_guide_traits
  - tft_guide_items
  - tft_guide_augments
- Each split guide table identifies one row by domain key + patchVersion.
- Existing key + patchVersion rows are updated; missing rows are inserted.
- Split guide tables do not use soft delete.
- Do not use or reintroduce legacy guides, augment_guide_rewards, or augment_guide_plans tables.
</guide-curation>

<patch-note-curation>
- Manual patch-note CRUD uses patch_notes and patch_note_changes.
- `patch_note_changes` is the final admin/runtime table. `patch_changes` is legacy migration terminology only and must not be used in current admin/API docs.
- Patch note version identifies one patch note.
- isCurrent must be unique among active, non-deleted patch notes.
- Creating/updating current=true must unset other active current patch notes.
- When the database has a single-current unique index, current patch updates should persist the old rows as not-current before saving the new current row.
- highlightsJson and tagsJson are JSON string arrays.
- Patch changes belong to one patch note.
- Admin patch-note delete is soft delete for the patch note row.
- Patch-change delete is hard delete in the current implementation.
- Deleting a patch note soft-deletes the patch note row and hard-deletes its patch changes.
- Manual updates to imported patch notes or changes mark manuallyEdited=true.
- Riot official import endpoint is POST /api/admin/patch-notes/import/riot.
- Import request fields are sourceUrl, locale, version, and current.
- If sourceUrl is omitted, backend discovers the latest official TFT patch note from the configured Riot tag page.
- Import preserves manuallyEdited rows on later imports.
- Imported highlights should be normalized so Riot source numbering such as `(6)` and redundant date/patch prefixes do not appear in public summary cards.
- Patch-note scheduler exists but must stay disabled in local/dev unless explicitly enabled.
</patch-note-curation>

<safety-rules>
- Admin pages should not call APIs for placeholder member/community pages.
- JSON fields must be validated before persistence.
- Soft-delete is preferred for curation data where deletedAt exists.
- Swagger annotations belong in XxxControllerDocs interfaces, not directly in controllers.
- Import controls must clearly separate manual curation from crawler/import behavior.
- Forceful destructive cleanup of imported data should not be added to admin UI without a separate explicit spec.
</safety-rules>

</spec>
