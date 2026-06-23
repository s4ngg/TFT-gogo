<spec domain="admin">

<purpose>
Admin pages manage TFTgogo curation data, imports, and protected operations.
All admin backend endpoints are protected by X-Admin-Token.
</purpose>

<routes>
- /admin -> admin login.
- /admin/decks -> meta deck curation.
- /admin/hero-augments -> hero augment deck curation.
- /admin/guides -> game guide manual curation and CDragon import.
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
- frontend/src/pages/Admin/AdminPatchNotes.tsx
- frontend/src/pages/Admin/AdminMembers.tsx
- frontend/src/pages/Admin/AdminCommunity.tsx
- frontend/src/pages/Admin/Admin.module.css
- frontend/src/api/adminApi.ts
</frontend-structure>

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

- GET /api/admin/guides?guideType=&patchVersion=&active=
- POST /api/admin/guides
- POST /api/admin/guides/import/cdragon
- PATCH /api/admin/guides/{guideId}
- DELETE /api/admin/guides/{guideId}

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

<guide-curation>
- Manual guide CRUD still uses the legacy guides table.
- guides is identified by guideType + targetKey + patchVersion.
- Manual guide delete is soft delete through active=false and deletedAt.
- Manual guide dataJson must be a valid JSON object.
- CDragon import is separate from manual guide CRUD and writes to split tables:
  - tft_guide_champions
  - tft_guide_traits
  - tft_guide_items
  - tft_guide_augments
  - augment_guide_plans
- CDragon import endpoint is POST /api/admin/guides/import/cdragon.
- CDragon import request requires patchVersion and can include setNumber, mutator, includeChampions, includeTraits, includeItems, and includeAugments.
- Public guide reads prefer split tables and use guides only as fallback when split rows are absent.
</guide-curation>

<patch-note-curation>
- Manual patch-note CRUD uses patch_notes and patch_changes.
- Patch note version identifies one patch note.
- isCurrent must be unique among active, non-deleted patch notes.
- Creating/updating current=true must unset other active current patch notes.
- highlightsJson and tagsJson are JSON string arrays.
- Patch changes belong to one patch note.
- Admin delete is soft delete.
- Manual updates to imported patch notes or changes mark manuallyEdited=true.
- Riot official import endpoint is POST /api/admin/patch-notes/import/riot.
- Import request fields are sourceUrl, locale, version, and current.
- If sourceUrl is omitted, backend discovers the latest official TFT patch note from the configured Riot tag page.
- Import preserves manuallyEdited rows on later imports.
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
