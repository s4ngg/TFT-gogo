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
  -> soft-delete patch note and hard-delete its patch changes.
- GET /api/admin/patch-notes/{patchNoteId}/changes
  -> admin patch-change list for a patch note.
- POST /api/admin/patch-note-changes
  -> create patch change manually.
- PATCH /api/admin/patch-note-changes/{changeId}
  -> update patch change manually.
- DELETE /api/admin/patch-note-changes/{changeId}
  -> hard-delete patch change.
- POST /api/admin/patch-notes/import/riot
  -> import latest or specified official Riot/TFT patch note.
</backend>
<frontend>
- frontend/src/api/patchNotes.ts -> public patch-note API functions.
- frontend/src/api/patchNoteStatsPayload.ts -> nested stats payload reader.
- frontend/src/api/adminApi.ts -> admin patch note/change/import functions with admin JWT Authorization bearer header.
- frontend/src/hooks/usePatchNotes.ts -> TanStack Query hooks.
- frontend/src/pages/PatchNotes/hooks/ -> public page state and query wiring.
- frontend/src/pages/PatchNotes/components/ -> public patch-note sections.
- frontend/src/pages/Admin/AdminPatchNotes.tsx -> admin page wrapper.
- frontend/src/pages/Admin/components/AdminPatchNotesManager.tsx -> admin curation/import UI.
</frontend>
</api>

<public-behavior>
- Public page selects the active current patch by default.
- Public `GET /api/patch-notes` returns a recent public history window, not the full admin patch-note table.
- The current backend public history window is 6 months (`PUBLIC_PATCH_HISTORY_MONTHS=6` in PatchNoteServiceImpl).
- Public history must include every non-deleted patch with publishedAt inside the 6-month cutoff and must also include
  the active current patch even when it is outside that cutoff.
- Public history ordering must place the active current patch first, then remaining patches by publishedAt desc and id desc.
- The public history repository contract is `findPublicHistorySinceIncludingCurrent(cutoff)`, where `cutoff` means the
  lower publishedAt bound for the recent-history window.
- If current is not available, the list order must still make the latest patch easy to select.
- The public list endpoint returns ApiResponse<List<PatchNoteResponse>> and does not include full change rows.
- PatchNoteResponse exposes current status as both the user-facing status label and the isCurrent/current field
  used by the frontend default-selection resolver.
- Patch changes endpoint returns ApiResponse<PatchChangePageResponse>.
- PatchChangePageResponse contains items, page, pageSize, totalItems, totalPages, and stats.
- stats are calculated for the selected patch version using the active type/impact/query filters, but not limited to the currently filtered page.
- Category counts intentionally remain available across categories so the category tabs can show useful counts while one category is selected.
- Frontend normalization must prefer payload.stats before falling back to legacy top-level stats.
- Change filters are category, type, impact, query, page, and pageSize.
- Backend accepts page=1..10000 and pageSize=1..1000 for patch changes.
- Current public frontend requests patch changes with pageSize=1000. #623 owns the future UX/performance policy to reduce this or introduce a clearer paged/whole-list mode.
- Search/filter changes must not reload the whole page or reset the selected patch unexpectedly.
- Public search input is debounced before requesting patch changes and resets the change page to 1 only when query/category/selected patch changes.
- Public API calls must go through frontend/src/api/patchNotes.ts and TanStack Query hooks.
- Components must not import axios or fetch directly.
- The UI should group and display patch changes in a readable form, with long rows wrapped safely on desktop and mobile.
- The public page must not show crawler/import metadata such as importedAt or source diagnostics.
- Public summary/highlight labels must strip Riot source numbering or date/patch prefixes such as `(6)`, `(5)`, and redundant patch title fragments before display.
</public-behavior>

<data-model>
<patch_notes>
- version identifies one patch note.
- title, summary, content, focus, representative image, highlights, publishedAt, isCurrent, createdAt, updatedAt, and deletedAt are public/curation fields.
- source_key, source_url, source_locale, import_source, imported_at, and manually_edited are crawler/import metadata.
- isCurrent must be unique among active, non-deleted patch notes.
- Creating or updating a current patch note must unset other active current patch notes in the same transaction.
- Public history query is supported by idx_patch_notes_history(deleted_at, published_at, id). The query still uses
  current-first ordering with a CASE expression, so very large patch-note tables may need a future execution-plan review.
- Add or change patch-note indexes only through a new forward Flyway migration. Do not edit V1__init_schema.sql
  or any other already-merged/applied V*.sql file to add, remove, or rewrite patch-note indexes.
- Keep the local-smoke schema snapshot aligned with the final migrated schema, but treat it as QA/reference data
  only. The forward migration is what protects existing production databases.
</patch_notes>

<patch_note_changes>
- Final runtime/ERD table name is `patch_note_changes`. Do not introduce new code or docs that use `patch_changes` as the current table name.
- Older migrations may mention `patch_changes` only to rename legacy databases to `patch_note_changes`.
- patch_note_id links each row to patch_notes.
- category enum values: CHAMPION, TRAIT, ITEM, AUGMENT, SYSTEM.
- change type enum values: BUFF, NERF, ADJUST, NEW.
- impact enum values: HIGH, MEDIUM, LOW.
- targetKey, targetName, summary, beforeValue, afterValue, imageUrl, tagsJson, sortOrder, createdAt, and updatedAt are public/curation fields.
- source_key, source_heading_path, source_order, imported_at, and manually_edited are crawler/import metadata.
- Imported row duplicate detection uses patchNote + sourceKey.
- Manual rows may have null sourceKey.
- PatchChange rows do not use soft delete or deleted_at in the current implementation.
</patch_note_changes>
</data-model>

<admin-rules>
- Admin endpoints under /api/admin/** require an admin JWT bearer token, except /api/admin/auth/login, /api/admin/auth/refresh, and /api/admin/auth/logout.
- Admin patch-note read endpoints allow ADMIN_MASTER, ADMIN_EDITOR, and ADMIN_VIEWER.
- Admin patch-note writes, deletes, Riot import, and patch-change writes/deletes require ADMIN_MASTER or ADMIN_EDITOR.
- Manual admin creates/updates validate JSON arrays such as highlights and tags.
- Manual admin updates to imported patch notes or changes must mark manuallyEdited=true.
- Imported rows with manuallyEdited=true must be preserved on later imports.
- Admin patch-note delete is soft delete for the PatchNote row.
- PatchChange delete is hard delete in the current implementation.
- Deleting a patch note soft-deletes the PatchNote row and hard-deletes its PatchChange rows.
- Admin patch change forms must reject empty sortOrder text before numeric conversion.
- Editing a patch change must not drift across selected patch notes. If selected patch note changes while editing, clear edit state before save.
- When marking an imported or manually created patch as current, clear existing current rows and flush before inserting/updating the new current row if the database has a single-current unique index.
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
- Imported highlights are derived from parsed section labels. They must be normalized to remove source numbering, date prefixes, patch-version prefixes, and generic wrapper text before saving when possible.
- PatchNote upsert matching order is sourceKey, then sourceUrl, then version.
- PatchChange upsert matching key is patchNote + sourceKey.
- Imported patch notes/changes with manuallyEdited=false may be updated by re-import.
- Imported patch notes/changes with manuallyEdited=true are skipped by re-import.
- Stale imported changes may be hard-deleted when a re-import updates an existing patch note.
- Import validation runs before patch metadata, current status, or change rows are mutated.
- Empty parsed rows and fatal parser warnings such as `max detail rows reached` reject the entire import.
- Existing imported patch rows are protected by `app.patch-note.crawler.min-retained-row-ratio` (default `0.5`).
  The retained-row ratio is the share of existing, automatically managed sourceKeys that also exist in the new
  import. Manually edited rows and rows without a sourceKey are excluded from the denominator. A re-import below
  that ratio fails without changing existing data, while smaller reductions and same-patch hotfix additions
  continue through the normal upsert/stale-delete flow.
- Current implementation does not expose dryRun or forceOverwrite. Add those as a separate enhancement if needed.
- Scheduler-driven latest refresh uses the same import endpoint/service path with sourceUrl=null and version=null.
  It must re-run the latest import even when the latest patch already exists locally so current marking and official
  Riot content updates are not skipped.
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
  - Imported rows may also use same-patch Guide champion/trait names as a secondary signal when heading/group context is weak.
- Change type inference is conservative:
  - NEW for clearly new/added content.
  - Current Riot import maps bug fixes, mixed changes, unclear numeric direction, and all non-new rows to ADJUST.
  - BUFF/NERF are supported enum values for manual/admin curation, but the current Riot importer does not infer them automatically.
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
  - history-months=6
  - list-scan-limit=50
  - list-cron=0 0 * * * *
  - refresh-cron=0 30 6 * * *
  - zone=Asia/Seoul
- Startup import runs on ApplicationReadyEvent only when enabled=true and startup-import=true.
- Startup import runs before guide startup import so guide patch-version=latest can resolve to the latest current patch note.
- Startup import is a two-step sequence under the `startup` lock:
  1. call latest refresh first (`importLatestPatchNote`) with sourceUrl=null/version=null/current=properties.current.
  2. run recent-history list backfill (`importUnknownPatchNotesFromList`).
- The latest refresh step is required even when the latest patch already exists locally. Do not replace startup import
  with list-only import; list-only import can skip an already-imported latest patch and miss current correction or Riot
  official content updates.
- If the latest refresh step fails, the startup task is logged as failed by the scheduler wrapper and the list backfill
  does not run in that same startup execution. The hourly list-check and next daily refresh can recover later.
- List check runs hourly by default and imports unknown official list items within list-scan-limit and history-months.
- Daily refresh runs at 06:30 KST by default and refreshes the latest patch note.
- Daily refresh imports the latest official patch note directly. It must not skip the latest patch merely because
  that patch version/sourceUrl is already present locally; re-import keeps existing imported rows up to date.
- List check fetches the official Riot/TFT tag page, parses list items, and scans at most
  `min(properties.listScanLimit, listItems.size())` items.
- List check computes one history cutoff per execution: `now - properties.historyMonths`.
- List check scans the selected window in reverse index order (`scanLimit - 1` down to `0`) so older unknown patches are
  imported first and the newest list item (`index == 0`) is processed last.
- List check skips official list items with no detailUrl.
- List check skips official list items whose publishedAt cannot be parsed. Unknown dates must not be treated as
  recent because a Riot markup/locale parsing break could otherwise import up to list-scan-limit old patches.
- List check skips official list items older than the history-months cutoff.
- List check treats an item as already imported if sourceKey, sourceUrl, or resolved version already exists.
- List check only marks the newest list item current when `properties.current=true` and `index == 0`. Older backfilled
  items, including items that fail during import, must be requested with current=false.
- List check isolates import failures per item. If one official detail page fails, log detailUrl/version/current
  context and continue with the remaining list items so a bad older item does not block later items or the latest/current import.
- Scheduler uses an in-process AtomicBoolean lock to avoid overlapping imports in a single server instance.
- Multi-server deployments require a future shared DB/Redis lock before enabling scheduler on multiple instances.
- Local/dev should keep scheduler disabled by default. Use manual admin import for local QA.
</scheduler>

<frontend-rules>
- Public PatchNotes page should default to the latest/current patch.
- Frontend normalization must preserve `isCurrent` and map both `status=CURRENT` and `isCurrent=true` to the
  user-facing current status.
- Frontend default patch selection must prefer the API current patch version. The first list item is only a fallback
  when the API does not expose a current patch.
- Search input must not cause a full page reload on every keystroke.
- Search input should be debounced and preserve selected patch/category context while only refreshing the change-list query.
- Type/category filters should not expose confusing BUFF/NERF/ADJUST badges when the product decision is to simplify display.
- Long patch change rows must wrap safely and avoid horizontal overflow.
- Mobile PatchNotes layout must not horizontally overflow. The side rail, history panel, category tabs, search box,
  content grid, summary grid, and change panel must fit inside the viewport. Category tabs wrap on mobile instead
  of creating a nested horizontal scroller.
- Repeated or redundant title/detail text should be collapsed or formatted into title + value lines where possible.
- Quick insight/summary labels should be derived from cleaned highlight labels and deduplicated before display.
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
- Admin service tests should cover patch-note CRUD, patch-change CRUD, JSON array validation, duplicate/current behavior, not found errors, patch-note soft delete, patch-change hard delete, and manuallyEdited marking.
- Import tests should cover latest import by tag page, direct sourceUrl import, repeated import idempotency, manuallyEdited skip, sourceKey matching, stale imported change handling, parser warnings, unsupported host rejection, current flag behavior, and Guide-name-assisted category inference.
- Import safety tests should cover empty rows, fatal/truncated parser warnings, sourceKey retention drops even when
  row counts are unchanged, manually edited row exclusion, and allowed small reductions.
- Scheduler tests should cover disabled state, startup-import flag, list scan limit, already-imported skip, current flag, and in-process lock skip.
- Scheduler tests should also cover:
  - startup refresh of the latest patch even when the latest item is already imported.
  - daily latest refresh even when already imported.
  - history-months cutoff skip for old list items.
  - publishedAt=null list-item skip.
  - per-item import failure continuation.
  - failed older list item requested with current=false and latest list item requested with current=true.
- Frontend tests should cover readPatchChangeStatsPayload, current patch default selection, search without full reload,
  simplified public display, and mobile no-overflow behavior.
- Browser QA should cover /patch-notes at 390px width with app shell scrollLeft=0, document/body scrollWidth equal
  to viewport width, and no overflow in side rail/history/filter/change panels.
- Parser tests must use saved reduced Riot HTML snapshots under backend/src/test/resources/patchnote/crawl/ and must not depend on live official pages.
- Patch-note DB changes must include migration QA: inspect the diff for historical V*.sql edits, run a clean-DB
  Flyway migrate plus backend startup, and directly verify the new index/table/column in MySQL when applicable.
</validation>

</spec>
