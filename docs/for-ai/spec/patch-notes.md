<spec domain="patch-notes">

<purpose>
Patch Notes provides public TFT patch history, selected patch detail, searchable change rows, admin curation, Riot official import, and scheduled refresh.
Page: /patch-notes.
</purpose>

<routes>
- /patch-notes -> public patch-note page. Latest/current patch is selected by default.
- /admin/patch-notes -> admin patch-note curation and import page.
</routes>

<api>
<backend>
- GET /api/patch-notes
  -> fetch public patch-note history list.
- GET /api/patch-notes/{version}/changes?category=&type=&impact=&query=&page=&pageSize=
  -> fetch selected patch changes.
- GET /api/admin/patch-notes
  -> admin patch-note list.
- POST /api/admin/patch-notes
  -> create patch note manually.
- PATCH /api/admin/patch-notes/{patchNoteId}
  -> update patch note manually.
- DELETE /api/admin/patch-notes/{patchNoteId}
  -> soft-delete patch note and its changes.
- GET /api/admin/patch-notes/{patchNoteId}/changes
  -> admin patch-change list for a patch note.
- POST /api/admin/patch-note-changes
  -> create patch change manually.
- PATCH /api/admin/patch-note-changes/{changeId}
  -> update patch change manually.
- DELETE /api/admin/patch-note-changes/{changeId}
  -> soft-delete patch change.
- POST /api/admin/patch-notes/import/riot
  -> import latest or specified official Riot/TFT patch note.
</backend>
<frontend>
- frontend/src/api/patchNotes.ts -> public patch-note API functions.
- frontend/src/api/patchNoteStatsPayload.ts -> nested stats payload reader.
- frontend/src/api/adminApi.ts -> admin patch note/change/import functions with X-Admin-Token.
- frontend/src/hooks/usePatchNotes.ts -> TanStack Query hooks.
- frontend/src/pages/PatchNotes/hooks/ -> public page state and query wiring.
- frontend/src/pages/PatchNotes/components/ -> public patch-note sections.
- frontend/src/pages/Admin/AdminPatchNotes.tsx -> admin page wrapper.
- frontend/src/pages/Admin/components/AdminPatchNotesManager.tsx -> admin curation/import UI.
</frontend>
</api>

<public-behavior>
- Public page selects the active current patch by default.
- If current is not available, the list order must still make the latest patch easy to select.
- The public list endpoint returns ApiResponse<List<PatchNoteResponse>> and does not include full change rows.
- Patch changes endpoint returns ApiResponse<PatchChangePageResponse>.
- PatchChangePageResponse contains items, page, pageSize, totalItems, totalPages, and stats.
- stats are calculated for the selected patch version as a whole, not for the currently filtered page.
- Frontend normalization must prefer payload.stats before falling back to legacy top-level stats.
- Change filters are category, type, impact, query, page, and pageSize.
- Search/filter changes must not reload the whole page or reset the selected patch unexpectedly.
- Public API calls must go through frontend/src/api/patchNotes.ts and TanStack Query hooks.
- Components must not import axios or fetch directly.
- The UI should group and display patch changes in a readable form, with long rows wrapped safely on desktop and mobile.
- The public page must not show crawler/import metadata such as importedAt or source diagnostics.
</public-behavior>

<data-model>
<patch_notes>
- version identifies one patch note.
- title, summary, content/description, focus, representative image, highlights, publishedAt, isCurrent, createdAt, updatedAt, and deletedAt are public/curation fields.
- source_key, source_url, source_locale, import_source, imported_at, and manually_edited are crawler/import metadata.
- isCurrent must be unique among active, non-deleted patch notes.
- Creating or updating a current patch note must unset other active current patch notes in the same transaction.
</patch_notes>

<patch_changes>
- patch_note_id links each row to patch_notes.
- category enum values: CHAMPION, TRAIT, ITEM, AUGMENT, SYSTEM.
- change type enum values: BUFF, NERF, ADJUST, NEW.
- impact enum values: HIGH, MEDIUM, LOW.
- targetKey, targetName, summary, beforeValue, afterValue, imageUrl, tagsJson, sortOrder, createdAt, updatedAt, and deletedAt are public/curation fields.
- source_key, source_url, source_heading_path, source_order, source_locale, imported_at, import_source, and manually_edited are crawler/import metadata.
- Imported row duplicate detection uses patchNote + sourceKey.
- Manual rows may have null sourceKey.
</patch_changes>
</data-model>

<admin-rules>
- Admin endpoints are protected by X-Admin-Token through /api/admin/**.
- Manual admin creates/updates validate JSON arrays such as highlights and tags.
- Manual admin updates to imported patch notes or changes must mark manuallyEdited=true.
- Imported rows with manuallyEdited=true must be preserved on later imports.
- Admin delete is soft delete. Do not hard-delete patch notes or patch changes from normal admin flows.
- Deleting a patch note soft-deletes its active/non-deleted patch changes.
- Admin patch change forms must reject empty sortOrder text before numeric conversion.
- Editing a patch change must not drift across selected patch notes. If selected patch note changes while editing, clear edit state before save.
</admin-rules>

<riot-import>
- POST /api/admin/patch-notes/import/riot is implemented.
- Request DTO: AdminPatchNoteImportRequest.
- Request fields:
  - sourceUrl: optional official Riot/TFT detail URL. If omitted, import discovers the latest detail URL from the official tag/list page.
  - locale: optional, default from PatchNoteCrawlerProperties, normally ko-kr.
  - version: optional override when title/URL parsing is insufficient.
  - current: optional, default true. When true, imported patch becomes current and other current patches are unset.
- Response DTO: AdminPatchNoteImportResponse.
- Response fields:
  - patchNoteId
  - version
  - sourceUrl
  - patchNoteCreated
  - patchNoteUpdated
  - patchNoteSkipped
  - createdChanges
  - updatedChanges
  - skippedChanges
  - parserWarnings
- Import source policy: official Riot/TFT pages only.
- Allowed exact hosts are www.leagueoflegends.com and teamfighttactics.leagueoflegends.com.
- The importer must not become a generic external URL fetch proxy.
- If sourceUrl is omitted, fetch the configured tag page and import the first valid detail URL.
- Detail fetch parses the Riot Next.js __NEXT_DATA__ payload.
- Parser implementation uses Jsoup and crawler DTOs, not regex-only HTML parsing.
- PatchNote upsert matching order is sourceKey, then sourceUrl, then version.
- PatchChange upsert matching key is patchNote + sourceKey.
- Imported patch notes/changes with manuallyEdited=false may be updated by re-import.
- Imported patch notes/changes with manuallyEdited=true are skipped by re-import.
- Stale imported changes may be soft-deleted when a re-import updates an existing patch note.
- Current implementation does not expose dryRun or forceOverwrite. Add those as a separate enhancement if needed.
</riot-import>

<crawler-parser>
- List page source: https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/
- Detail pages are official teamfighttactics.leagueoflegends.com game-updates URLs.
- Fetcher output: rawHtml, sourceUrl, fetchedAt, and httpStatus.
- List parser reads articleCardGrid items from __NEXT_DATA__.
- Detail parser reads articleMasthead and patchNotesRichText from __NEXT_DATA__.
- PatchNoteCrawlDocument contains sourceUrl, locale, contentId, title, version, summary, publishedAt, imageUrl, authors, sections, rows, and parserWarnings.
- PatchChangeCrawlRow contains sourceKeyCandidate/sourceKeyHash, headingPath, sourceOrder, sectionTitle, groupTitle, rowText, rawHtml, beforeText, afterText, and parserWarnings.
- Rows with one clear before/after arrow may set beforeValue and afterValue.
- Rows with multiple or ambiguous arrows must preserve the full summary and avoid unsafe splitting.
- Category inference is conservative:
  - SYSTEM for bug fix/system/loot/mode/ranked/encounter context.
  - CHAMPION, TRAIT, ITEM, AUGMENT only when heading/group/row context is clear.
- Change type inference is conservative:
  - NEW for clearly new/added content.
  - ADJUST for bug fixes, mixed changes, unclear numeric direction, or low confidence.
  - BUFF/NERF only when implemented confidence is sufficient.
- Impact defaults to MEDIUM.
</crawler-parser>

<scheduler>
- PatchNoteImportScheduler is implemented.
- Scheduler properties use prefix app.patch-note.scheduler.
- Defaults:
  - enabled=false
  - startup-import=false
  - locale=ko-kr
  - current=true
  - list-scan-limit=5
  - list-cron=0 0 * * * *
  - refresh-cron=0 30 6 * * *
  - zone=Asia/Seoul
- Startup import runs on ApplicationReadyEvent only when enabled=true and startup-import=true.
- List check runs hourly by default and imports unknown official list items within list-scan-limit.
- Daily refresh runs at 06:30 KST by default and refreshes the latest patch note.
- Scheduler uses an in-process AtomicBoolean lock to avoid overlapping imports in a single server instance.
- Multi-server deployments require a future shared DB/Redis lock before enabling scheduler on multiple instances.
- Local/dev should keep scheduler disabled by default. Use manual admin import for local QA.
</scheduler>

<frontend-rules>
- Public PatchNotes page should default to the latest/current patch.
- Search input must not cause a full page reload on every keystroke.
- Type/category filters should not expose confusing BUFF/NERF/ADJUST badges when the product decision is to simplify display.
- Long patch change rows must wrap safely and avoid horizontal overflow.
- Repeated or redundant title/detail text should be collapsed or formatted into title + value lines where possible.
- ImportedAt/source diagnostics should stay hidden in public UI.
- Admin import controls should live in /admin/patch-notes, not in the public page.
- Existing manual CRUD must keep working after import controls are added.
</frontend-rules>

<backend-structure>
- Public controller: backend/src/main/java/com/tftgogo/domain/patchnote/controller/PatchNoteController.java
- Admin controller: backend/src/main/java/com/tftgogo/domain/patchnote/controller/AdminPatchNoteController.java
- Swagger docs: backend/src/main/java/com/tftgogo/domain/patchnote/controller/docs/
- Request DTOs: backend/src/main/java/com/tftgogo/domain/patchnote/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/patchnote/dto/response/
- Crawler DTOs: backend/src/main/java/com/tftgogo/domain/patchnote/dto/crawl/
- Services: backend/src/main/java/com/tftgogo/domain/patchnote/service/
- Implementations: backend/src/main/java/com/tftgogo/domain/patchnote/service/impl/
- Scheduler: backend/src/main/java/com/tftgogo/domain/patchnote/scheduler/PatchNoteImportScheduler.java
- Config: PatchNoteCrawlerProperties and PatchNoteImportSchedulerProperties.
- Entities: PatchNote and PatchChange.
- Repositories: PatchNoteRepository and PatchChangeRepository.
</backend-structure>

<validation>
- Public service tests should cover list response, latest/current behavior, version not found, filter query, stats separation, page slicing, invalid pagination, enum parsing, and LIKE escaping.
- Admin service tests should cover patch-note CRUD, patch-change CRUD, JSON array validation, duplicate/current behavior, not found errors, soft delete, and manuallyEdited marking.
- Import tests should cover latest import by tag page, direct sourceUrl import, repeated import idempotency, manuallyEdited skip, sourceKey matching, stale imported change handling, parser warnings, unsupported host rejection, and current flag behavior.
- Scheduler tests should cover disabled state, startup-import flag, list scan limit, already-imported skip, current flag, and in-process lock skip.
- Frontend tests should cover readPatchChangeStatsPayload, latest patch default selection, search without full reload, simplified public display, and mobile no-overflow behavior.
- Parser tests must use saved reduced Riot HTML snapshots under backend/src/test/resources/patchnote/crawl/ and must not depend on live official pages.
</validation>

</spec>
