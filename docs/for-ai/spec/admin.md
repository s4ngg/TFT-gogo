<spec domain="admin">

<purpose>
Admin-only curation and maintenance surface.
Page: Admin (/admin).
</purpose>

<routes>
- /admin -> admin management dashboard
</routes>

<api>
<backend>
- GET    /api/admin/decks                    -> list all decks with curation state
- POST   /api/admin/decks/meta/aggregate     -> trigger meta deck re-aggregation
- PATCH  /api/admin/decks/{deckId}           -> update deck curation
- DELETE /api/admin/decks/{deckId}/curation  -> reset deck curation data

- GET    /api/admin/guides                   -> list guide entries
- POST   /api/admin/guides                   -> create guide entry
- POST   /api/admin/guides/import/cdragon    -> Community Dragon에서 챔피언/특성 가이드 항목을 가져옵니다
- PATCH  /api/admin/guides/{guideId}         -> update guide entry
- DELETE /api/admin/guides/{guideId}         -> soft delete guide entry

- GET    /api/admin/patch-notes               -> list patch notes
- POST   /api/admin/patch-notes               -> create patch note
- PATCH  /api/admin/patch-notes/{patchNoteId} -> update patch note
- DELETE /api/admin/patch-notes/{patchNoteId} -> soft delete patch note
- POST   /api/admin/patch-note-changes        -> create patch change
- PATCH  /api/admin/patch-note-changes/{changeId} -> update patch change
- DELETE /api/admin/patch-note-changes/{changeId} -> soft delete patch change
</backend>
<frontend>
- frontend/src/api/adminApi.ts
</frontend>
</api>

<auth>
- Authentication: X-Admin-Token request header.
- Current implementation uses AdminTokenFilter for /api/admin/**.
- Swagger uses the X-Admin-Token API key security scheme.
- Future plan: migrate to ROLE_ADMIN-based Spring Security.
- Never expose admin endpoints without the token check.
- Admin auth failure response shape is a known follow-up item until it uses the common ApiResponse format.
</auth>

<business-rules>
- Admin APIs are curation tools. They should not bypass public-domain validation rules.
- Admin delete operations should prefer soft delete when the domain table has active/deletedAt.
- Request validation belongs in Request DTOs and Service-level guards for important invariants.
- Swagger annotations belong in XxxControllerDocs interfaces, not directly in Controllers.
</business-rules>

<deck-curation>
- Admin can override the auto-generated deck name.
- Admin can hide a deck (marks as hidden; does NOT delete the data).
- Admin can set manual display order for decks.
- Admin can assign hex-board positions per champion per deck level.
- If positions are set, deck detail uses them instead of CDragon auto-layout.
- Positions are stored per deck signature so they survive re-aggregation.
- Curation data key: deck signature (top-2 trait combination).
</deck-curation>

<guide-curation>
- Admin can create, update, list, and soft delete guide entries.
- 관리자는 CDragon 챔피언/특성 가이드 항목을 동일한 guides 테이블로 가져올 수 있다.
- guideType + targetKey + patchVersion identifies one guide entry.
- Import는 동일한 guideType + targetKey + patchVersion을 가진 미삭제 행의 콘텐츠를 수정하되 기존 active 상태는 유지하고, 없는 행은 active=true로 새로 생성한다.
- Soft-deleted guide rows still reserve guideType + targetKey + patchVersion unless the schema changes.
- dataJson must be a JSON object.
- active=false hides an entry from public guide responses.
</guide-curation>

<patch-note-curation>
- Admin can create, update, list, and soft delete patch notes.
- Admin can create, update, and soft delete patch changes.
- isCurrent must stay unique among active, non-deleted patch notes.
- highlightsJson and tagsJson must be JSON string arrays.
</patch-note-curation>

<frontend-structure>
- frontend/src/pages/Admin/
</frontend-structure>

</spec>
