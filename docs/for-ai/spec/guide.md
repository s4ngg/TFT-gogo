<spec domain="guide">

<purpose>
Game guide page providing trait, item, augment, and champion reference data.
Page: Guide (/guide).
</purpose>

<routes>
- /guide      -> guide landing (default tab)
- /guide/:tab -> specific tab (traits / items / augments / champions)
- /admin/guides -> admin CDragon guide import page
</routes>

<api>
<backend>
- GET  /api/guide       -> fetch public guide catalog
- GET  /api/guide/patch-version -> fetch only the current/latest guide patch version without loading catalog entries
- GET  /api/guide/{tab} -> fetch public guide data for one tab
- GET  /api/guide/{tab}?patchVersion=&query=&page=&pageSize=&sortKey=&sortDir=&cost=
- POST /api/admin/guides/import/cdragon -> import champion/trait/item/augment guide rows from Community Dragon
- GET  /api/cdragon/tft/ko-kr -> backend-proxied CDragon TFT locale JSON for frontend name/trait/item resolution
</backend>
<frontend>
- frontend/src/api/guide.ts            -> main guide API calls
- frontend/src/api/guideClient.ts      -> HTTP client wrapper for guide
- frontend/src/pages/Guide/guideFallbackData.ts -> fallback data when API is unavailable
- frontend/src/api/guideNormalizers.ts -> normalize raw guide data before use
- frontend/src/api/guideTypes.ts       -> TypeScript types for guide domain
- frontend/src/hooks/useGuide.ts       -> TanStack Query hooks for guide patch version and tab pages
- frontend/src/pages/Guide/hooks/      -> page UI state, tab pagination, metric sorting, and dialog state
- frontend/src/pages/Guide/components/ -> page-specific guide panels/cards/controls
</frontend>
</api>

<business-rules>
- Guide covers TRAIT, ITEM, AUGMENT, and CHAMPION data.
- Public tab path values are traits, items, augments, champions. Invalid tabs throw GUIDE_INVALID_TAB.
- Public responses use ApiResponse&lt;GuideCatalogResponse&gt; for /api/guide and ApiResponse&lt;GuidePageResponse&lt;GuideEntryResponse&gt;&gt; for /api/guide/{tab}.
- GuideEntryResponse includes id, guideType, targetKey, name, summary, imageUrl, patchVersion, sortOrder, dataJson.
- dataJson must serialize as a JSON object, not a raw JSON string.
- The legacy unified `guides` table is not part of the current contract and must not be reintroduced for admin CRUD or public fallback.
- Admin manual guide CRUD is removed from the backend contract. Admin guide writes currently happen only through /api/admin/guides/import/cdragon.
- Admin endpoints are protected by X-Admin-Token through /api/admin/**.
- CDragon import is upsert-based per split table. Same domain key + patchVersion updates the existing row; missing rows are created.
- Split guide tables do not use soft delete. If deletion/restoration policy is needed later, design it explicitly per table.
- If patchVersion is omitted on the public catalog, the latest patch is resolved from split guide tables.
- The public /api/guide/patch-version endpoint resolves only the current/latest guide patch version and must not load
  catalog entries. It exists so the Guide page can show the patch 기준 and request tab data without an expensive
  upfront catalog query.
- If patchVersion is omitted on a tab endpoint, the latest patch is resolved from that tab's table.
- CDragon import accepts patchVersion=`latest`. The backend resolves it from the current patch note first, then from the latest non-deleted patch note. If no patch note exists, import fails with INVALID_INPUT; local QA should import patch notes first or pass an explicit guide patchVersion.
- Admin guide import UI defaults patchVersion to `latest` so guide data aligns with the current patch-note version when patch notes are available.
- Latest patch selection sorts patchVersion numerically by major/minor parts, then lexicographically as a final tie-breaker.
- Public page defaults are page=1 and pageSize=10. page must be 1..10000 and pageSize must be 1..100.
- Public sortKey is optional and limited to avgPlace, pickRate, top4, winRate. Current static CDragon rows do not provide production metrics; missing metrics sort last and default ordering is used.
- Public default ordering is Korean name collation, then sortOrder, then id.
- cost filter is valid only as 1..5 and applies only to champion data.
- query searches name, summary, and targetKey in memory after loading the resolved split-table patch.
- If no patch exists, /api/guide returns an empty catalog and /api/guide/{tab} returns an empty page with totalPages=1.
- Invalid persisted JSON fields are treated as GUIDE_INVALID_DATA and should be fixed in import/storage logic, not hidden by public API fallback.
- CDragon import request fields: patchVersion (required, max 20, supports `latest`), setNumber (default 17), mutator (default TFTSet{setNumber}), includeChampions, includeTraits, includeItems, includeAugments.
- CDragon import request flag behavior: omitted includeChampions/includeTraits default to true; omitted includeItems/includeAugments default to false at request DTO level, so admin UI and scheduler must send explicit true when items/augments should be imported.
- CDragon import rejects requests where includeChampions, includeTraits, includeItems, and includeAugments all resolve to false.
- CDragon import response fields: createdCount, updatedCount, skippedCount, championCount, traitCount, itemCount, augmentCount, importedCount (= createdCount + updatedCount).
- CDragon item import includes only completed craftable TFT_Item_* rows with exactly two composition components and no associatedTraits.
- CDragon item import excludes component-only rows, emblem/trait-associated rows, radiant/artifact/support/non-craftable rows that do not match the completed-item policy.
- CDragon augment import reads only the requested set/mutator augments and requires apiName, name, description, and icon.
- CDragon augment import excludes debug/dummy/test/placeholder/inactive/disabled entries by apiName/name/description keywords.
- CDragon static import does not fetch Riot matches and does not compute production guide metrics.
- CDragon text sanitization removes unresolved placeholders/tokens. Template tokens such as `{{...}}` must not leak to public UI. If a token mapping is explicitly implemented later, expand that token to user-facing text with import tests.
- #393 owns the separate guide metric strategy. CDragon provides static names/descriptions/images/tags/combinations; Riot match detail stored in cached_match can later provide avgPlace, pickRate, TOP4 rate, winRate, and sampleCount through a dedicated refresh path.
- Metric refresh must keep "-" and [] fallbacks when an item or augment is below the minimum sample size; do not invent numbers from insufficient samples.
- Data originates from CDragon where possible; use communityDragonAssets.ts helpers for frontend images.
- guideFallbackData.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
- Public guide UI should request only the patch version through useGuideCatalog/useGuidePatchVersion-style query,
  then request tab data through useGuideTabItems. Components must not fetch guide data directly.
- GET /api/guide remains available for compatibility, but the Guide page should not prefetch the full catalog on
  every load when only patchVersion is needed.
- Public guide UI includes the GameGuide AI entry points on guide cards. The AI behavior itself is owned by
  gameguide-ai-pathfinder.md, but Guide cards are responsible for opening the widget with the selected
  guideType/targetKey/name ref.
- On mobile widths, Guide cards and section headers must never expand wider than the viewport. Trait cards use
  minmax(0, 1fr) internal tracks, wrap tip rows, and keep card-level AI buttons inside the card grid.
- The top app layout must not inherit horizontal scroll from the mobile nav or top status badges. Guide page
  mobile QA should assert app shell scrollLeft=0 and document/body scrollWidth equals viewport width.
</business-rules>

<database-contract>
<tft-guide-champions>
- Table: tft_guide_champions
- Columns: id, champion_key, name, cost, role, position, image_url, stats_json, traits_json, best_items_json, patch_version, created_at, updated_at
- Unique: champion_key + patch_version
- Index: patch_version + cost + id
</tft-guide-champions>

<tft-guide-traits>
- Table: tft_guide_traits
- Columns: id, trait_key, name, type, icon_url, tone, summary, levels_json, tier_effects_json, champions_json, special_units_json, tips_json, patch_version, created_at, updated_at
- Unique: trait_key + patch_version
- Index: patch_version + id
</tft-guide-traits>

<tft-guide-items>
- Table: tft_guide_items
- Columns: id, item_key, name, category, image_url, description, stats_json, best_users_json, combinations_json, patch_version, created_at, updated_at
- Unique: item_key + patch_version
- Index: patch_version + id
</tft-guide-items>

<tft-guide-augments>
- Table: tft_guide_augments
- Columns: id, augment_key, name, description, icon_url, tags_json, stats_json, patch_version, created_at, updated_at
- Unique: augment_key + patch_version
- Index: patch_version + name + id
</tft-guide-augments>

<removed-legacy>
- Do not use: guides
- Do not use: augment_guide_rewards
- Do not use: augment_guide_plans
</removed-legacy>
</database-contract>

<data-contracts>
<guide-entry>
- guideType: TRAIT | ITEM | AUGMENT | CHAMPION
- targetKey: stable domain key such as CDragon apiName
- name: display name
- summary: short display text
- imageUrl: optional public image URL
- dataJson: tab-specific JSON object built from split table columns
- patchVersion: patch scope such as 17.3
- sortOrder: derived display order for API response
</guide-entry>

<cdragon-champion-data-json>
- cost: 1..5
- role: CDragon role text or fallback text
- position: imported position label
- traits: string[]
- bestItems: [] at current import stage
- stats: { ad, armor, attackSpeed, hp, mana, mr, range }
</cdragon-champion-data-json>

<cdragon-trait-data-json>
- count: max minUnits from CDragon effects
- type: synergy label
- summary: sanitized CDragon desc
- tone: bronze | silver | gold | prismatic, derived from max effect style
- levels: effect minUnits list, using "N+" when maxUnits is open-ended
- tierEffects: [{ level, description }]
- champions: [{ cost, imageUrl, name }]
- specialUnits: [{ imageUrl, name, note }]
- tips: [] at current import stage
- specialUnits is required for summon/generated trait units such as Dark Star's small black hole. Such units must not appear as normal champions.
</cdragon-trait-data-json>

<cdragon-item-data-json>
- category: completed item category label
- description: sanitized CDragon desc
- bestUsers: [] at current import stage
- combinations: [{ label, note, items: [{ imageUrl, name }] }]
</cdragon-item-data-json>

<cdragon-augment-data-json>
- description: sanitized CDragon desc/description/tooltip
- tags: displayable CDragon tags plus description-derived tags, or ["공용"]
</cdragon-augment-data-json>
</data-contracts>

<backend-implementation>
- GuideController owns public read endpoints under /api/guide.
- GuideServiceImpl resolves patch versions from split repositories, validates query params, parses split JSON columns, sorts responses, and builds GuidePageResponse.
- AdminGuideController owns only /api/admin/guides/import/cdragon for guide writes.
- GuideCdragonImportServiceImpl fetches CommunityDragonProperties.tftKoKrUrl using RestTemplate and builds split-table guide candidates.
- CDragon set data resolution first searches root.setData by setNumber + mutator, then falls back to root.sets[setNumber] if champions and traits exist.
- Champion import includes only shop champions whose apiName starts with TFT{setNumber}_, cost is 1..5, has at least one trait, and has a display name.
- Trait import skips traits with no matching shop champion references.
- Generated/summon units are excluded from champion rows and attached to trait special_units_json when explicitly mapped.
- Current special unit mapping includes TFT17_DarkStar_FakeUnit as a special unit under TFT17_DarkStar.
- Import asset URLs use CommunityDragonProperties.assetBaseUrl plus a lowercased asset path with .tex replaced by .png.
- Import sanitization removes HTML tags, resolves known @placeholder@ values, collapses whitespace, and trims text.
</backend-implementation>

<scheduler>
- GuideCdragonImportScheduler is implemented.
- Scheduler properties use prefix app.guide.cdragon.
- Defaults:
  - enabled=false
  - startup-import=false
  - patch-version=latest
  - set-number unset; omitted requests auto-select the latest TFT set from CDragon data
  - mutator unset; explicit set-number defaults to TFTSet{setNumber}
  - include-champions=true
  - include-traits=true
  - include-items=true
  - include-augments=true
  - sync-cron=0 10 * * * *
  - refresh-cron=0 40 6 * * *
  - zone=Asia/Seoul
- Startup import runs on ApplicationReadyEvent only when enabled=true and startup-import=true.
- Startup import runs after patch-note startup import so patch-version=latest can resolve to the latest current patch note.
- Sync import runs hourly by default and refresh import runs daily at 06:40 KST by default.
- Scheduler uses an in-process AtomicBoolean lock to avoid overlapping guide imports in a single server instance.
- Scheduler also uses a MySQL advisory DB lock so only one server instance runs CDragon import in multi-server deployments.
- Local/dev should keep the scheduler disabled by default. Use the admin import button for local QA.
</scheduler>

<backend-structure>
- Controller: backend/src/main/java/com/tftgogo/domain/guide/controller/
- Swagger docs: backend/src/main/java/com/tftgogo/domain/guide/controller/docs/
- Request DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/guide/service/ and service/impl/
- Repositories: backend/src/main/java/com/tftgogo/domain/guide/repository/
- Entities: backend/src/main/java/com/tftgogo/domain/guide/entity/
- CDragon config: backend/src/main/java/com/tftgogo/global/cdragon/config/CommunityDragonProperties.java
- CDragon locale proxy/cache: backend/src/main/java/com/tftgogo/global/cdragon/controller/CDragonController.java and backend/src/main/java/com/tftgogo/global/cdragon/service/TftAssetCacheService.java
- Scheduler: backend/src/main/java/com/tftgogo/domain/guide/scheduler/GuideCdragonImportScheduler.java
- Scheduler properties: backend/src/main/java/com/tftgogo/global/config/GuideCdragonImportProperties.java
</backend-structure>

<validation>
- Service-layer unit tests are the primary backend verification target.
- Public guide tests should cover tab parsing, page/pageSize bounds, sortKey/sortDir validation, cost filtering, JSON object response, latest patch fallback, empty latest patch behavior, split champion/trait filtering, and CDragon import split-table upsert.
- Import tests should assert importedCount semantics through createdCount + updatedCount, not championCount + traitCount.
- #393 metric refresh tests should be added with the dedicated refresh implementation: cached match fixture aggregation, queue/patch filtering, duplicate match handling, minimum sample fallback, sampleCount, and idempotent guide metric updates.
- Frontend/browser QA should cover the /guide mobile layout at 390px width: trait cards, section headers,
  champion rows, tip rows, and GameGuide AI card buttons must have no right overflow.
- GameGuide AI card-click smoke QA should verify the widget opens with the selected card ref and, when logged out,
  shows only the auth-required message defined in gameguide-ai-pathfinder.md.
</validation>

<data-ingestion>
- Current stage: CDragon champion/trait/item/augment static guide import into split guide tables.
- Recommended local QA order: apply schema, import patch notes/current patch, then run admin guide CDragon import with patchVersion=`latest`.
- Current import does not curate bestItems/tips and should not be treated as final editorial guide quality.
- Next #393 stage is to decide and implement the metric data source/collection policy, then compute guide metrics in a separate refresh path from CDragon static import.
- AI server/FastAPI is not required for guide CRUD. Add it only when AI/RAG/recommendation behavior needs guide data.
</data-ingestion>

</spec>
