<spec domain="patch-notes">

<purpose>
Patch note browsing, version history, and curated patch-change management.
Page: PatchNotes (/patch-notes).
</purpose>

<routes>
- /patch-notes -> patch note list + selected patch detail
</routes>

<api>
<backend>
- GET /api/patch-notes                           -> fetch public patch history list
- GET /api/patch-notes/{version}/changes         -> fetch change details for a specific version
- GET    /api/admin/patch-notes                  -> admin patch note list
- POST   /api/admin/patch-notes                  -> create patch note
- PATCH  /api/admin/patch-notes/{patchNoteId}    -> update patch note
- DELETE /api/admin/patch-notes/{patchNoteId}    -> soft delete patch note
- GET    /api/admin/patch-notes/{patchNoteId}/changes -> admin patch change list
- POST   /api/admin/patch-note-changes           -> create patch change
- PATCH  /api/admin/patch-note-changes/{changeId} -> update patch change
- DELETE /api/admin/patch-note-changes/{changeId} -> soft delete patch change
</backend>
<frontend>
- frontend/src/api/patchNotes.ts
- frontend/src/api/patchNoteStatsPayload.ts -> isolates nested stats payload reading for tests
- frontend/src/hooks/usePatchNotes.ts -> TanStack Query hooks for patch history and patch changes
- frontend/src/pages/PatchNotes/hooks/usePatchNotesPageState.ts -> UI filter/search/page/expanded state
- frontend/src/pages/PatchNotes/hooks/usePatchChangesPage.ts -> change query wiring and page bounds correction
- frontend/src/pages/PatchNotes/components/ -> public patch note page sections
- frontend/src/pages/Admin/AdminPatchNotes.tsx -> admin patch-note page wrapper
- frontend/src/pages/Admin/components/AdminPatchNotesManager.tsx -> admin patch note and patch change CRUD screen
- frontend/src/api/adminApi.ts -> admin patch note/change request functions and admin-token headers
</frontend>
</api>

<business-rules>
- Latest/current patch note is shown by default on page load.
- Patch history list lets users navigate to previous patches.
- Patch note list response uses ApiResponse&lt;List&lt;PatchNoteResponse&gt;&gt; and does not include full changes.
- Patch change response uses ApiResponse&lt;PatchChangePageResponse&gt; and includes items, page, pageSize, totalItems, totalPages, stats.
- PatchChangePageResponse.stats is nested under the response payload. Frontend normalization must prefer payload.stats before falling back to legacy top-level stats.
- stats are calculated from the selected patch version as a whole, not from the currently filtered page.
- Public PatchNotes UI owns only composition and event wiring. Filter/search/page/expanded state belongs in usePatchNotesPageState, and change-page query/page correction belongs in usePatchChangesPage.
- Public PatchNotes API calls must go through frontend/src/api/patchNotes.ts and TanStack Query hooks; components must not fetch directly.
- Public PatchNotes fallback images may use temporary set-specific CDragon asset paths, but new production data should prefer backend-provided imageUrl or shared CDragon asset helpers/config.
- Changes are categorized by backend enum values: CHAMPION, TRAIT, ITEM, AUGMENT, SYSTEM.
- Change type enum values are BUFF, NERF, ADJUST, NEW.
- Impact enum values are HIGH, MEDIUM, LOW.
- version param format matches backend storage key, for example "17.3".
- Local DB smoke data lives in patch_notes and patch_changes. patch_notes.version identifies one patch note.
- Admin endpoints are protected by X-Admin-Token through /api/admin/**.
- Admin reads/writes use /api/admin/patch-notes and /api/admin/patch-note-changes.
- Admin patch change list uses patchNoteId and returns deletedAt-is-null changes, including inactive rows for curation.
- The admin patch-note screen can list, create, update, and soft-delete patch notes and patch changes. Keep crawler/import work separate from this curation contract.
- Admin delete uses soft delete through active/deletedAt. Do not hard delete patch note or patch change rows.
- highlightsJson and tagsJson must validate as JSON string arrays.
- isCurrent must stay unique among active, non-deleted patch notes.
- When a patch note is created or updated with current=true, existing active current patch notes must be unset.
- DB-level protection should also enforce a single active current patch note. If schema management is manual, apply the matching SQL migration/index before production-like testing.
</business-rules>

<backend-structure>
- Public controller: backend/src/main/java/com/tftgogo/domain/patchnote/controller/PatchNoteController.java
- Public Swagger docs: backend/src/main/java/com/tftgogo/domain/patchnote/controller/docs/PatchNoteControllerDocs.java
- Admin controller: backend/src/main/java/com/tftgogo/domain/patchnote/controller/AdminPatchNoteController.java
- Admin Swagger docs: backend/src/main/java/com/tftgogo/domain/patchnote/controller/docs/AdminPatchNoteControllerDocs.java
- Request DTOs: backend/src/main/java/com/tftgogo/domain/patchnote/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/patchnote/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/patchnote/service/ and service/impl/
- Repositories: PatchNoteRepository, PatchChangeRepository
- Entities: PatchNote, PatchChange
</backend-structure>

<backend-implementation>
- PatchNoteServiceImpl owns public patch note list and selected-version change queries.
- Public change stats are calculated from all active, non-deleted changes for the selected patch note.
- Public change items are filtered by category, type, impact, and escaped LIKE query, then returned with page metadata.
- Current curated-data implementation may slice filtered results in the service. If crawler/import substantially increases patch_changes volume, move filtered item paging/counting to repository-level Pageable/count queries.
- AdminPatchNoteServiceImpl owns patch note CRUD, patch change list lookup, patch change CRUD, JSON array serialization/validation, current-patch clearing, and soft delete.
- Creating or updating a patch note with current=true must unset other active current patch notes in the same transaction.
- Deleting a patch note must soft-delete its active/non-deleted patch changes.
</backend-implementation>

<validation>
- Public service tests should cover list response, version not found, filtered change query, stats separation, page slicing, invalid pagination, empty filters, enum parsing, and LIKE escaping.
- Admin service tests should cover patch note CRUD, admin patch change list lookup, patch change CRUD, JSON array validation, duplicate/current behavior, not found errors, and soft delete.
- Frontend tests should continue to cover nested stats payload handling via readPatchChangeStatsPayload.
- Admin frontend tests should cover admin API request shape, admin-token headers, request error wrapping, and core form payload mapping before crawler/import is added.
- Swagger smoke testing should verify public APIs without admin token and admin APIs with X-Admin-Token.
</validation>

<data-ingestion>
- Current stage: public PatchNotes browsing, backend admin CRUD, admin patch change list lookup, and frontend AdminPatchNotes curation screen are implemented for curated DB/admin data.
- Next stage before crawling/import: merge the admin screen/API work, keep the spec in sync, and repeat smoke testing against the admin endpoints.
- Patch-note crawling/import comes after admin UI wiring, current-patch uniqueness, public stats contracts, and smoke testing are stable in develop.
- External crawling/import must write into the same patch_notes and patch_changes contract used by admin curation.
- AI server/FastAPI is not required for patch-note CRUD. Add it only when AI summarization/search/RAG behavior needs patch-note data.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/PatchNotes/
</frontend-structure>

</spec>
