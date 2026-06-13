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
- Admin patch change forms must reject empty sortOrder text before numeric conversion; do not allow blank input to be stored as 0.
- Editing a patch change must not drift across selected patch notes. If the selected patch note changes while editing, clear the edit state before saving.
- Admin patch-note API functions must include X-Admin-Token headers and wrap request failures so auth failures can still be detected by callers.
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

<crawler-import-plan>
- Production import source is the official Riot/TFT patch-note site, not fixture/seed files.
- Tag/list page: https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/
- Detail pages are linked from the tag page through articleCardGrid items. Current links point to https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/... patch-note detail URLs.
- The Riot pages are Next.js pages. Fetch HTML first and extract the __NEXT_DATA__ application/json script. Do not hardcode _next/data buildId values because they change on deploy.
- List extraction should read props.pageProps.page.blades[type=articleCardGrid].items. Each item currently includes title, publishedAt, description.body, imageMedia.url, analytics.contentId, and action.payload.url.
- Detail extraction should read props.pageProps.page where type=patchNote. The articleMasthead blade contains title, description.body, publishDate, tags, authors, and banner data.
- Detail body extraction should read the patchNotesRichText blade. Its richText is currently {type: "html", body: "..."} and the body contains div#patch-notes-container.
- The detail HTML structure observed on 17.2, 17.1, 15.9, and 14.24 patch notes uses h2 for major sections, h4.change-detail-title for change groups, ul/li for change rows, and span.change-indicator for before/after arrows.
- Crawler implementation must be split into fetcher, parser, normalizer, and importer steps so official page markup changes can be isolated from DB write logic.
- Backend implementation should mirror the guide import pattern: service interfaces under domain/patchnote/service, implementations under service/impl, request/response DTOs under dto/request and dto/response, and Swagger docs in AdminPatchNoteControllerDocs.
- Fetcher should use the backend RestTemplate pattern with explicit timeout, redirect handling, user-agent, and logged failures. It should return raw HTML plus sourceUrl and fetchedAt metadata.
- Parser should use a real HTML parser such as Jsoup rather than regex-only parsing. Regex may be used only for small post-processing like version extraction. If adding Jsoup is considered too much dependency surface, keep parser scope smaller and document the lower reliability.
- Parser should convert the Next.js page data into PatchNoteCrawlDocument and PatchChangeCrawlRow records before domain normalization.
- Normalizer should convert crawler rows into PatchNoteImportCandidate and PatchChangeImportCandidate records before touching entities.
- Planned admin entrypoint: POST /api/admin/patch-notes/import/crawl protected by X-Admin-Token. The request should include sourceUrl, version, locale, dryRun, and forceOverwrite or equivalent review-safe options.
- Default locale is ko-kr. If ko-kr parsing fails, en-us pages with the same slug may be used only for diagnostics or parser comparison, not for Korean public copy.
- The crawler parser should extract patch title, version, publishedAt, summary, highlights, masthead image, authors, h2 section headings, h4 group headings, li row text, source ordering, before/after arrow text, and row source HTML when available.
- PatchNote title should prefer masthead title. Version should prefer explicit request.version, then title/slug extraction such as teamfight-tactics-patch-17-2 -> 17.2.
- PatchNote summary should prefer masthead description.body with HTML stripped. highlights should use a small curated subset from patch highlight sections only when confidently parsed; otherwise leave empty for admin curation.
- PatchChange category should be inferred from current h2/h4 heading and target text. Korean headings such as "특성", "유닛"/"챔피언", "증강", "상징"/"아이템", "체계", and "버그 수정" must map conservatively to TRAIT, CHAMPION, AUGMENT, ITEM, SYSTEM, and SYSTEM.
- PatchChange type should be inferred from row text and before/after values only when confidence is high. Buff/nerf keywords and numeric direction may map to BUFF or NERF; mixed changes, bug fixes, new content, removals, and uncertain rows should map to ADJUST or NEW with review-required tagging when needed.
- Impact should default to MEDIUM unless section context or manually curated rules justify HIGH or LOW. Do not invent impact values outside HIGH, MEDIUM, LOW.
- targetName should be parsed from the text before the first colon when present, or from parentheses/heading context when reliable. targetKey should be a normalized slug from category + targetName until CDragon ID matching is implemented.
- beforeValue and afterValue should be extracted from span.change-indicator-separated text when a row has one clear before/after pair. If a row has multiple arrows or mixed values, keep the full text in summary and leave beforeValue/afterValue null unless the parser can split them safely.
- imageUrl should come from masthead/list image for PatchNote. PatchChange imageUrl should be null unless the row contains a direct image or a later CDragon matching step provides a verified asset URL.
- PatchNote upsert key is version. Importing the same version twice must not create duplicate patch_notes rows.
- PatchChange upsert must use a stable sourceKey derived from official source structure when available, such as contentId + heading path + source order. If no stable key exists, use a deterministic hash from sourceUrl, version, heading path, category, targetName, normalized row text, and source order.
- Imported rows that cannot be confidently classified into CHAMPION, TRAIT, ITEM, AUGMENT, or SYSTEM must be marked for admin review and should not be exposed as active public changes until curated.
- Imported rows that cannot be confidently mapped to BUFF, NERF, ADJUST, or NEW should default to ADJUST with a review-required tag rather than inventing unsupported enum values.
- Import must preserve admin curation. Manual edits should not be overwritten by a repeated crawl unless forceOverwrite is explicitly requested.
- Import result should report sourceUrl, version, dryRun, created, updated, skipped, reviewRequired, failed counts, parserWarnings, and row-level errors when available.
- Automatic scheduling is out of scope until manual/admin-triggered crawling is stable.
</crawler-import-plan>

<validation>
- Public service tests should cover list response, version not found, filtered change query, stats separation, page slicing, invalid pagination, empty filters, enum parsing, and LIKE escaping.
- Admin service tests should cover patch note CRUD, admin patch change list lookup, patch change CRUD, JSON array validation, duplicate/current behavior, not found errors, and soft delete.
- Crawler/import service tests should use saved official-page HTML snapshots and mock HTTP fetches; tests must not depend on live official page availability.
- Crawler snapshot tests should include the tag page and at least three detail pages with different structures, such as 17.2, 17.1, 15.9, and 14.24.
- Crawler list tests should assert extraction from articleCardGrid items: title, publishedAt, description.body, imageMedia.url, analytics.contentId, and action.payload.url.
- Crawler detail tests should assert extraction from articleMasthead and patchNotesRichText: title, description, publishDate, authors, richText.body, h2 sections, h4 groups, li rows, and change-indicator arrows.
- Crawler/import tests should cover parser success, missing __NEXT_DATA__, missing articleCardGrid, missing patchNotesRichText, parser markup drift, repeated import idempotency, sourceKey/hash generation, review-required rows, manual edit preservation, dryRun behavior, and forceOverwrite behavior.
- Crawler parser tests should verify that rows with multiple change-indicator arrows preserve the full summary and avoid unsafe beforeValue/afterValue splitting.
- Frontend tests should continue to cover nested stats payload handling via readPatchChangeStatsPayload.
- Admin frontend tests should cover admin API request shape, admin-token headers for patch note and patch change read/write calls, request error wrapping, and core form payload mapping before crawler/import is added.
- Admin UI validation should be verified around empty sortOrder handling and patch-change edit state reset when the selected patch note changes.
- Swagger smoke testing should verify public APIs without admin token and admin APIs with X-Admin-Token.
</validation>

<data-ingestion>
- Current stage: public PatchNotes browsing, backend admin CRUD, admin patch change list lookup, and frontend AdminPatchNotes curation screen are implemented for curated DB/admin data.
- Next stage before crawling/import: merge the admin screen/API work, keep the spec in sync, and repeat smoke testing against the admin endpoints.
- Patch-note crawling/import comes after admin UI wiring, current-patch uniqueness, public stats contracts, and smoke testing are stable in develop. The implementation direction is official Riot/TFT patch-note crawling, not manual fixture/seed import.
- External crawling/import must write into the same patch_notes and patch_changes contract used by admin curation, with additional source metadata or equivalent duplicate-detection strategy when needed.
- AI server/FastAPI is not required for patch-note CRUD. Add it only when AI summarization/search/RAG behavior needs patch-note data.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/PatchNotes/
</frontend-structure>

</spec>
