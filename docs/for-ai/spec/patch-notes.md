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
- POST   /api/admin/patch-notes/import/crawl     -> planned write-side official Riot/TFT patch-note import endpoint; not present in current develop
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
- Current develop has no admin patch-note crawler/import UI yet.
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
- PatchNote and PatchChange entities include crawler/import metadata fields in current develop: sourceUrl, sourceLocale, importSource, importedAt, and manuallyEdited.
- PatchChange also includes sourceKey, sourceHeadingPath, and sourceOrder for imported-row duplicate detection and diagnostics.
- Manual admin updates mark imported patch notes or patch changes as manuallyEdited so later imports can preserve curated edits.
- Current develop includes crawler fetcher/parser infrastructure and tests, but no DB write-side import service, import endpoint, dryRun/forceOverwrite request DTO, or admin import UI.
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
- Import source enum: backend/src/main/java/com/tftgogo/domain/patchnote/entity/PatchNoteImportSource.java
- Crawler config: backend/src/main/java/com/tftgogo/domain/patchnote/config/PatchNoteCrawlerProperties.java
- Current crawler services: PatchNoteCrawlerFetchService, PatchNoteCrawlerParser
- Current crawler implementations: RiotPatchNoteCrawlerFetchServiceImpl, JsoupPatchNoteCrawlerParser
- Current crawler DTOs: PatchNoteCrawlFetchedPage, PatchNoteCrawlListItem, PatchNoteCrawlDocument, PatchChangeCrawlRow
</backend-structure>

<backend-implementation>
- PatchNoteServiceImpl owns public patch note list and selected-version change queries.
- Public change stats are calculated from all active, non-deleted changes for the selected patch note.
- Public change items are filtered by category, type, impact, and escaped LIKE query, then returned with page metadata.
- Current curated-data implementation may slice filtered results in the service. If crawler/import substantially increases patch_changes volume, move filtered item paging/counting to repository-level Pageable/count queries.
- AdminPatchNoteServiceImpl owns patch note CRUD, patch change list lookup, patch change CRUD, JSON array serialization/validation, current-patch clearing, and soft delete.
- Creating or updating a patch note with current=true must unset other active current patch notes in the same transaction.
- Deleting a patch note must soft-delete its active/non-deleted patch changes.
- AdminPatchNoteServiceImpl marks imported rows as manuallyEdited when an admin edits them through normal CRUD paths.
- RiotPatchNoteCrawlerFetchServiceImpl fetches official Riot/TFT pages only, using PatchNoteCrawlerProperties for tagUrl, locale, user-agent, and timeouts.
- RiotPatchNoteCrawlerFetchServiceImpl allows exact official hosts only: www.leagueoflegends.com and teamfighttactics.leagueoflegends.com.
- JsoupPatchNoteCrawlerParser parses the Next.js __NEXT_DATA__ payload from official list/detail pages into crawler DTOs.
- JsoupPatchNoteCrawlerParser generates a fixed-length sourceKeyHash for rows from normalized source material.
- Current develop does not include PatchNoteCrawlerImportService or POST /api/admin/patch-notes/import/crawl yet.
</backend-implementation>

<crawler-research-2026-06-14>
- Official source policy: use Riot/TFT official patch-note pages only. Do not build the production importer from local seed/fixture data.
- Tag/list page checked: https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/
- Detail pages checked:
  - https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-2/
  - https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-17-1/
  - https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-15-9-notes/
  - https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/teamfight-tactics-patch-14-24-notes/
- Raw HTML check confirmed that both list and detail pages include script#__NEXT_DATA__.
- List page JSON path confirmed:
  - props.pageProps.page.blades contains riotbar, textMasthead, articleCardGrid.
  - articleCardGrid.items[0] contains title, publishedAt, description.body, imageMedia.url, analytics.contentId, action.payload.url.
  - Example latest item at research time was 17.2 with publishedAt=2026-04-28T18:00:00.000Z and action.payload.url pointing to the teamfighttactics.leagueoflegends.com detail URL.
  - This 17.2 value is a research snapshot, not a hardcoded latest-patch rule. Runtime latest selection must sort official list items by publishedAt/current source data instead of assuming a fixed version.
- Detail page JSON path confirmed on 17.2:
  - props.pageProps.page.type=patchNote.
  - props.pageProps.page.blades contains riotbar, articleMasthead, patchNotesRichText, articleCardCarousel.
  - articleMasthead contains title, description.body, publishDate, authors[].name.
  - patchNotesRichText.richText.type=html and patchNotesRichText.richText.body contains the article body.
- Detail richText HTML check confirmed:
  - h2 section examples: 4월 30일 17.2 추가 패치 노트, 패치 하이라이트, 체계, 대규모 변경 사항, 소규모 변경 사항, 버그 수정.
  - h4.change-detail-title examples: 신의 선물, 증강, 특성, 유닛, 시작 조우자, 증강 등장 확률, 전리품 체계, 신의 은총.
  - li rows include plain text and before/after arrows such as "주문력 175/200/250 ⇒ 주문력 225/250/300".
  - Some rows contain more than one before/after pair. Parser must preserve full summary when safe splitting is not possible.
- jsoup dependency research:
  - Current official jsoup release checked on 2026-06-14: 1.22.2.
  - Gradle notation: implementation 'org.jsoup:jsoup:1.22.2'.
  - License: MIT.
  - Runtime dependencies: none required.
  - Java support: Java 8+, compatible with backend Java 17.
</crawler-research-2026-06-14>

<crawler-implementation-slices>
- PR 1, DB metadata contract: landed in current develop.
  - source/import metadata exists on patch_notes and patch_changes.
  - Public/admin CRUD behavior remains unchanged.
  - Manual admin edits mark imported rows as manuallyEdited.
- PR 2, fetcher/parser/snapshot: landed in current develop.
  - jsoup dependency is present.
  - HTTP fetch abstraction and parser DTOs are present.
  - Reduced HTML snapshots and parser/fetcher tests are present under backend/src/test/resources/patchnote/crawl/ and backend/src/test/java.
  - No DB writes are performed by the crawler parser/fetcher slice.
- PR 3, importer upsert/dryRun: planned next write-side slice.
  - Convert parser output into import candidates.
  - Match existing patch_notes by version and patch_changes by patchNoteId + sourceKey.
  - Implement dryRun, forceOverwrite, warning/error result reporting, and manual-edit skip rules.
- PR 4, admin endpoint/UI: planned after write-side importer.
  - Expose POST /api/admin/patch-notes/import/crawl.
  - Add admin API function and admin import controls.
  - Display created/updated/skipped/review-required/error counts before and after confirmed import.
- Low-priority follow-up:
  - Refresh patch-note fallback images and guide fallback sample assets after current PRs merge.
</crawler-implementation-slices>

<crawler-backend-structure>
- Current packages under backend/src/main/java/com/tftgogo/domain/patchnote/:
  - service/PatchNoteCrawlerFetchService.java
  - service/PatchNoteCrawlerParser.java
  - service/impl/RiotPatchNoteCrawlerFetchServiceImpl.java
  - service/impl/JsoupPatchNoteCrawlerParser.java
  - dto/crawl/PatchNoteCrawlFetchedPage.java
  - dto/crawl/PatchNoteCrawlListItem.java
  - dto/crawl/PatchNoteCrawlDocument.java
  - dto/crawl/PatchChangeCrawlRow.java
  - config/PatchNoteCrawlerProperties.java
- Still planned for write-side import:
  - service/PatchNoteCrawlerImportService.java
  - service/impl/PatchNoteCrawlerImportServiceImpl.java
  - dto/request/PatchNoteCrawlImportRequest.java
  - dto/response/PatchNoteCrawlImportResponse.java
  - dto/response/PatchNoteCrawlRowErrorResponse.java
  - dto/crawl/PatchNoteImportCandidate.java
  - dto/crawl/PatchChangeImportCandidate.java
- Config:
  - PatchNoteCrawlerProperties uses @ConfigurationProperties.
  - Properties cover tagUrl, defaultLocale, userAgent, connectTimeoutMillis, readTimeoutMillis, and maxDetailRows.
- Controller rule:
  - Add the import endpoint to AdminPatchNoteController.
  - Swagger annotations belong in AdminPatchNoteControllerDocs only.
  - Endpoint must return ResponseEntity<ApiResponse<PatchNoteCrawlImportResponse>>.
- HTTP rule:
  - Use a backend HTTP client pattern with explicit timeout and user-agent.
  - Log sourceUrl and high-level failure reason only. Do not log full official HTML bodies.
</crawler-backend-structure>

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
- Production parser implementations must use Jsoup for robust HTML extraction from articleCardGrid, articleMasthead, and patchNotesRichText. Regex may be used only for small post-processing like version extraction; do not implement the crawler parser as regex-only or string-splitting HTML parsing.
- Parser should convert the Next.js page data into PatchNoteCrawlDocument and PatchChangeCrawlRow records before domain normalization.
- Normalizer should convert crawler rows into PatchNoteImportCandidate and PatchChangeImportCandidate records before touching entities.
- Planned admin entrypoint: POST /api/admin/patch-notes/import/crawl protected by X-Admin-Token. The request should include sourceUrl, version, locale, dryRun, and forceOverwrite or equivalent review-safe options.
- Default locale is ko-kr. If ko-kr parsing fails, en-us pages with the same slug may be used only for diagnostics or parser comparison, not for Korean public copy.
- The crawler parser should extract patch title, version, publishedAt, summary, highlights, masthead image, authors, h2 section headings, h4 group headings, li row text, source ordering, before/after arrow text, and row source HTML when available.
- PatchNote title should prefer masthead title. Version should prefer explicit request.version, then title/slug extraction such as teamfight-tactics-patch-17-2 -> 17.2.
- PatchNote summary should prefer masthead description.body with HTML stripped. highlights should use a small curated subset from patch highlight sections only when confidently parsed; otherwise leave empty for admin curation.
- targetName should be parsed from the text before the first colon when present, or from parentheses/heading context when reliable. targetKey should be a normalized slug from category + targetName until CDragon ID matching is implemented.
- beforeValue and afterValue should be extracted from span.change-indicator-separated text when a row has one clear before/after pair. If a row has multiple arrows or mixed values, keep the full text in summary and leave beforeValue/afterValue null unless the parser can split them safely.
- imageUrl should come from masthead/list image for PatchNote. PatchChange imageUrl should be null unless the row contains a direct image or a later CDragon matching step provides a verified asset URL.
- Automatic scheduling is out of scope until manual/admin-triggered crawling is stable.
</crawler-import-plan>

<crawler-fetcher-contract>
- Input:
  - sourceUrl if an admin targets a specific detail page.
  - tagUrl if sourceUrl is omitted and the latest official TFT patch note must be discovered.
  - locale, default ko-kr.
- Output:
  - rawHtml.
  - sourceUrl after redirect resolution when available.
  - fetchedAt.
  - httpStatus.
- Failure handling:
  - DNS/connection/timeout/5xx should fail the import request with a BusinessException and a safe admin-facing message.
  - 404 or unsupported URL should fail fast before parsing.
  - Parser should not retry on malformed HTML; return parserWarnings/rowErrors where partial parsing is possible.
- URL policy:
  - Allow only https URLs with exact official hosts: www.leagueoflegends.com and teamfighttactics.leagueoflegends.com.
  - Validate host names by parsing URI components. Do not use contains() or broad endsWith() checks for official-host validation.
  - Reject arbitrary external hosts from admin input to avoid turning the endpoint into a generic fetch proxy.
</crawler-fetcher-contract>

<crawler-parser-output-contract>
- PatchNoteCrawlListItem:
  - title: list card title.
  - publishedAt: list card date as LocalDateTime-compatible ISO string.
  - description: description.body stripped to plain text.
  - imageUrl: imageMedia.url.
  - contentId: analytics.contentId, nullable.
  - detailUrl: action.payload.url.
- PatchNoteCrawlDocument:
  - sourceUrl, locale, contentId.
  - title, version, summary, publishedAt.
  - imageUrl.
  - authors: list of author names.
  - sections: ordered heading tree for diagnostics and stable sourceKey generation.
  - rows: ordered PatchChangeCrawlRow list.
- PatchChangeCrawlRow:
  - sourceKeyCandidate: raw key material such as contentId + headingPath + sourceOrder when contentId exists. This value is for hash generation only and should not be stored directly as a unique DB key.
  - sourceKeyHash: fixed-length deterministic hash generated from normalized sourceKeyCandidate material when available.
  - headingPath: h2/h3/h4 text path, joined with " > ".
  - sourceOrder: 0-based row order inside the detail document.
  - sectionTitle: nearest h2.
  - groupTitle: nearest h4.change-detail-title or nearest meaningful heading.
  - rowText: normalized plain text.
  - rawHtml: reduced row HTML for tests/debugging, not for logging.
  - beforeText and afterText only when one clear arrow pair exists.
  - parserWarnings: uncertain split, missing heading, unsupported nested list, or ambiguous category.
</crawler-parser-output-contract>

<crawler-normalization-contract>
- PatchNoteImportCandidate:
  - version: explicit request.version first, then title, then detail URL slug.
  - title: articleMasthead.title.
  - summary: articleMasthead.description.body stripped to plain text.
  - description: optional longer intro text. Avoid storing the whole article body.
  - focus: nullable, admin-curated by default unless a short confident focus can be derived.
  - imageUrl: articleMasthead/banner image or list image.
  - publishedAt: articleMasthead.publishDate, fallback list publishedAt.
  - current: false by default unless admin explicitly requests current.
  - highlights: only parse from a clear patch-highlight section; otherwise empty list.
  - tags: include source tags and review markers, not raw article HTML.
- PatchChangeImportCandidate:
  - category: conservative enum mapping from heading and row context.
  - type: conservative enum mapping from wording and before/after direction.
  - impact: MEDIUM by default.
  - targetName: text before the first colon when reliable, otherwise group/heading-derived target.
  - targetKey: normalized lowercase slug from category + targetName until verified CDragon mapping exists.
  - summary: full normalized row text.
  - beforeValue/afterValue: only set when there is exactly one clear before/after pair.
  - imageUrl: null by default.
  - active: false when review is required, true only when category/type parsing is confident.
  - tagsJson: include "import:riot", "locale:ko-kr", and "review:required" when needed.
</crawler-normalization-contract>

<crawler-import-contract>
- Planned endpoint is POST /api/admin/patch-notes/import/crawl. Treat it as planned until the write-side import implementation lands; existing admin CRUD must keep working without it.
- Request DTO fields:
  - sourceUrl: optional official Riot/TFT detail URL. If omitted, importer may discover the latest detail URL from the tag/list page.
  - version: optional major.minor value. If present, it overrides version parsed from the title or slug.
  - locale: optional locale string, default ko-kr.
  - dryRun: optional boolean, default true for first UI wiring and false only when the admin explicitly confirms import.
  - forceOverwrite: optional boolean, default false. When false, imported rows with manual edits must be preserved.
- Response DTO fields:
  - sourceUrl, version, locale, dryRun.
  - patchNoteId when a write was performed or an existing row was matched.
  - createdCount, updatedCount, skippedCount, reviewRequiredCount, failedCount.
  - parserWarnings: list of non-blocking parser warnings such as missing optional masthead image or uncertain heading mapping.
  - rowErrors: list of row-level failures with sourceKey, headingPath, sourceOrder, rowTextPreview, and reason.
- dryRun must perform fetch, parse, normalize, duplicate matching, and validation, but it must not create, update, soft-delete, reactivate, or unset current patch notes.
- forceOverwrite must affect only crawler-owned fields on imported patch notes and imported patch changes. It must not bypass enum validation, JSON validation, soft-delete policy, admin-token protection, current-patch uniqueness, or official URL validation.
</crawler-import-contract>

<crawler-parser-rules>
- Category mapping must be conservative:
  - TRAIT: headings or group titles containing 특성, 계열, 직업, origin, or class when the target is a trait.
  - CHAMPION: headings containing 유닛, 챔피언, 단계 유닛, tier unit, or champion.
  - AUGMENT: headings containing 증강, 영웅 증강, augment, or anomaly when the row describes an augment-like effect.
  - ITEM: headings containing 아이템, 상징, 유물, 찬란한, 지원 아이템, item, emblem, artifact, radiant, or support item.
  - SYSTEM: headings containing 체계, 시스템, 버그 수정, 전리품, 랭크, 모드, 시작 조우자, system, bug fix, loot, ranked, mode, or encounter.
- If a row can match multiple categories, prefer the nearest h4 group title over the h2 section title. If it is still ambiguous, mark it review-required and keep it inactive.
- Change type mapping must be conservative:
  - BUFF: explicit 상향/강화/증가/improved/increased wording, or a numeric direction that clearly improves the target.
  - NERF: explicit 하향/약화/감소/reduced/decreased wording, or a numeric direction that clearly weakens the target.
  - NEW: clearly new content such as 신규, 추가, new, or added.
  - ADJUST: mixed before/after changes, reworks, bug fixes, removals, unclear numeric direction, and any row where BUFF/NERF/NEW confidence is low.
- Impact should default to MEDIUM unless section context or manually curated rules justify HIGH or LOW. Do not invent impact values outside HIGH, MEDIUM, LOW.
- Review-required rows should include a stable marker such as review:required in tagsJson or equivalent import metadata, and should default to active=false until an admin confirms them.
- Do not infer TFT gameplay value too aggressively. For example, "마나 60 -> 50" may be a buff for a champion but the same numeric decrease can be a nerf in another context. If the parser cannot determine value direction from context, use ADJUST + review-required.
- Rows under bug-fix sections should usually be SYSTEM + ADJUST unless the text clearly targets a champion/trait/item and should be curated differently.
</crawler-parser-rules>

<crawler-persistence>
- Current PatchNote/PatchChange entities store crawler source metadata before write-side import.
- PatchNote metadata: sourceUrl, sourceLocale, importedAt, importSource, and manuallyEdited. Keep version as the upsert key.
- PatchChange metadata: sourceKey, sourceUrl, sourceHeadingPath, sourceOrder, sourceLocale, importedAt, importSource, and manuallyEdited.
- PatchNote upsert key is version. Importing the same version twice must not create duplicate patch_notes rows.
- PatchChange upsert must use a stable fixed-length sourceKey hash derived from official source structure when available. Preferred sourceKey format is a sha-256 hex hash of normalized sourceUrl, version, contentId, heading path, category, targetName, normalized row text, and source order.
- Do not store long headingPath or raw row text as the unique sourceKey. Store raw diagnostics separately in sourceHeadingPath/sourceOrder and keep sourceKey suitable for DB indexing.
- If MySQL index constraints are added, prefer a uniqueness guarantee for imported rows by patch_note_id + source_key while still allowing manually created rows without a sourceKey. If the database cannot express the desired partial uniqueness cleanly, enforce the imported-row uniqueness in service logic and cover it with tests.
- Manual admin edits must be preserved. Admin update endpoints currently mark imported PatchNote/PatchChange rows as manuallyEdited.
- PatchNote manuallyEdited=true must protect admin-edited title, summary, description, focus, imageUrl, highlightsJson, and current flag from normal re-import overwrites.
- Repeated import behavior:
  - existing imported patch note/change + manuallyEdited=false + forceOverwrite=false: update crawler-owned fields.
  - existing imported patch note/change + manuallyEdited=true + forceOverwrite=false: skip content overwrite and report skipped.
  - existing imported patch note/change + manuallyEdited=true + forceOverwrite=true: overwrite crawler-owned fields only after validation and report updated.
  - soft-deleted imported row: do not silently reactivate unless the request explicitly supports reactivation.
- Imported rows that cannot be confidently classified into CHAMPION, TRAIT, ITEM, AUGMENT, or SYSTEM must be marked for admin review and should not be exposed as active public changes until curated.
- Imported rows that cannot be confidently mapped to BUFF, NERF, ADJUST, or NEW should default to ADJUST with review-required tagging rather than inventing unsupported enum values.
</crawler-persistence>

<crawler-admin-ui-plan>
- Admin import controls should live inside the admin patch-note screen, not the public PatchNotes page.
- Minimum controls:
  - sourceUrl text input for a specific official detail URL.
  - version text input, optional override.
  - locale select with ko-kr default.
  - dryRun toggle, default on.
  - forceOverwrite toggle, default off and visually separated from dryRun.
  - import/dry-run button using existing admin token request flow.
- Result panel:
  - Show sourceUrl, version, dryRun, patchNoteId.
  - Show createdCount, updatedCount, skippedCount, reviewRequiredCount, failedCount.
  - Show parserWarnings and rowErrors in a compact table.
  - If dryRun=false succeeds, invalidate admin patch-note and selected patch-change queries.
- UX safety:
  - First click should default to dryRun.
  - Actual write import should require dryRun=false and a visible result preview or explicit admin confirmation.
  - forceOverwrite should explain that it overwrites crawler-owned fields only and does not bypass manual-edit protection unless backend allows it.
</crawler-admin-ui-plan>

<crawler-acceptance-criteria>
- A dry run against an official ko-kr detail URL returns parsed patch note metadata and row counts without creating or updating DB rows.
- Importing the same official detail URL twice does not create duplicate patch_notes or duplicate imported patch_changes.
- Manually edited imported patch notes and patch changes are not overwritten when forceOverwrite=false.
- Imported patch changes use fixed-length sourceKey hashes, while long heading paths and row text remain diagnostics only.
- Admin import endpoint accepts only https URLs from exact official hosts and rejects lookalike or arbitrary external hosts.
- Ambiguous rows are returned with review-required markers and are inactive by default.
- Parser tests pass using saved snapshots and do not depend on live Riot page availability.
- Admin import endpoint is protected by X-Admin-Token and rejects non-official sourceUrl hosts.
- Public PatchNotes endpoints keep their existing response shape after crawler metadata is added.
</crawler-acceptance-criteria>

<crawler-fixtures>
- Parser tests should store reduced, representative HTML snapshots under backend/src/test/resources/patchnote/crawl/ rather than relying on live Riot pages.
- Suggested snapshot set:
  - list-ko-kr-teamfight-tactics-patch-notes.html for the tag page.
  - detail-ko-kr-17-2.html for a recent detail page with current Set 17 style structure.
  - detail-ko-kr-17-1.html for adjacent same-set detail structure.
  - detail-ko-kr-15-9.html for an older detail structure.
  - detail-ko-kr-14-24.html for a cross-season detail structure.
- Store expected parser output beside snapshots as JSON files using the crawl DTO shape, for example detail-ko-kr-17-2.expected.json.
- Do not store full official article copies when a reduced snapshot containing __NEXT_DATA__, articleMasthead, patchNotesRichText, h2, h4.change-detail-title, li, and span.change-indicator is enough for parser coverage.
- Snapshot tests must assert ordering as well as values because sourceOrder feeds stable sourceKey generation.
</crawler-fixtures>

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
- Admin frontend tests should cover admin API request shape, admin-token headers for patch note and patch change read/write calls, request error wrapping, and core form payload mapping before crawler import UI is added.
- Admin UI validation should be verified around empty sortOrder handling and patch-change edit state reset when the selected patch note changes.
- Swagger smoke testing should verify public APIs without admin token and admin APIs with X-Admin-Token.
</validation>

<data-ingestion>
- Current stage: public PatchNotes browsing, backend admin CRUD, admin patch change list lookup, frontend AdminPatchNotes curation screen, crawler metadata fields, and crawler fetcher/parser are implemented.
- Next stage before public crawler use: implement write-side import with dryRun/forceOverwrite, then expose POST /api/admin/patch-notes/import/crawl and add admin import controls.
- Patch-note crawling/import direction is official Riot/TFT patch-note crawling, not manual fixture/seed import.
- External crawling/import must write into the same patch_notes and patch_changes contract used by admin curation, using source metadata and sourceKey duplicate detection.
- AI server/FastAPI is not required for patch-note CRUD. Add it only when AI summarization/search/RAG behavior needs patch-note data.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/PatchNotes/
</frontend-structure>

</spec>
