<spec domain="admin">

<purpose>
Admin pages and APIs for TFT-gogo content management.
Page scope: /admin/* with AdminLayout.
</purpose>

<routes>
- /admin                -> admin login
- /admin/decks          -> meta deck curation
- /admin/hero-augments  -> hero augment deck curation
- /admin/guides         -> game guide import/management surface
- /admin/patch-notes    -> patch note curation
- /admin/members        -> prepared screen
- /admin/community      -> prepared screen
</routes>

<layout>
- /admin/* uses AdminLayout instead of the public layout.
- AdminLayout = AdminSidebar + content area.
- AdminSidebar menu: decks, hero augments, game guides, patch notes, members, community, logout.
- Prepared menus should show a prepared-state screen and must not call unfinished APIs.
</layout>

<frontend-structure>
frontend/src/
- layouts/AdminLayout.tsx
- components/admin/AdminSidebar.tsx
- pages/Admin/AdminLogin.tsx
- pages/Admin/AdminDecks.tsx
- pages/Admin/AdminHeroAugments.tsx
- pages/Admin/AdminGuides.tsx
- pages/Admin/AdminPatchNotes.tsx
- pages/Admin/AdminMembers.tsx
- pages/Admin/AdminCommunity.tsx
- pages/Admin/Admin.module.css
</frontend-structure>

<api>
<backend>
- GET    /api/admin/decks                         -> deck list
- PATCH  /api/admin/decks/{deckId}                -> save deck curation
- DELETE /api/admin/decks/{deckId}/curation       -> reset deck curation

- GET    /api/admin/hero-augment-decks            -> hero augment deck list
- POST   /api/admin/hero-augment-decks            -> create hero augment deck
- PUT    /api/admin/hero-augment-decks/{id}       -> update hero augment deck
- DELETE /api/admin/hero-augment-decks/{id}       -> delete hero augment deck

- POST   /api/admin/guides/import/cdragon         -> import CDragon champion/trait/item/augment guide rows

- GET    /api/admin/patch-notes                   -> patch note list
- POST   /api/admin/patch-notes                   -> create patch note
- PATCH  /api/admin/patch-notes/{patchNoteId}     -> update patch note
- DELETE /api/admin/patch-notes/{patchNoteId}     -> soft delete patch note
- GET    /api/admin/patch-notes/{patchNoteId}/changes -> patch change list
- POST   /api/admin/patch-note-changes            -> create patch change
- PATCH  /api/admin/patch-note-changes/{changeId} -> update patch change
- DELETE /api/admin/patch-note-changes/{changeId} -> soft delete patch change
</backend>
<frontend>
- frontend/src/api/adminApi.ts
</frontend>
</api>

<auth>
- Admin APIs require the X-Admin-Token request header.
- Frontend stores the token in localStorage key tftgogo_admin_token.
- Missing or invalid token responses redirect the admin UI to /admin login.
- AdminTokenFilter protects /api/admin/**.
- Never log the admin token.
</auth>

<business-rules>
- Swagger annotations belong in XxxControllerDocs interfaces, not directly in controllers.
- Admin APIs should follow the public domain validation rules for curated data.
- JSON string fields such as highlightsJson or tagsJson must contain valid JSON.
- Prepared features should not call backend APIs until the feature is implemented.
</business-rules>

<deck-curation>
- Deck curation can edit display name, hidden state, sort priority, curator note, boardPositions, playGuide, and heroAugments.
- Curation data is identified by deck signature and rankFilter.
- Public deck APIs should only expose non-hidden curation.
</deck-curation>

<guide-curation>
- Admin game guide manual CRUD is not part of the current backend contract.
- Admin guide writes currently use only POST /api/admin/guides/import/cdragon.
- CDragon import writes champion/trait/item/augment guide data into split guide tables:
  tft_guide_champions, tft_guide_traits, tft_guide_items, tft_guide_augments.
- Each split guide table identifies one row by domain key + patchVersion.
- Existing key + patchVersion rows are updated; missing rows are inserted.
- Split guide tables do not use soft delete.
- Do not use or reintroduce legacy guides, augment_guide_rewards, or augment_guide_plans tables.
</guide-curation>

<patch-note-curation>
- Only one patch note should be current at a time.
- Patch changes belong to a patch note.
- Patch note official crawling/import is separate from manual CRUD and should be implemented behind X-Admin-Token.
- Re-imported crawler data should not overwrite administrator edits unless force overwrite behavior is explicitly requested.
</patch-note-curation>

<hero-augment-deck>
- Hero augment deck data is separate from MetaDeck.
- grade values: S / A / B / C / D.
- recommended=true rows are exposed by public APIs.
- champions and heroAugments fields are stored as JSON strings.
</hero-augment-deck>

</spec>
