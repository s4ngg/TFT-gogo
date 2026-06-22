<spec domain="guide">

<purpose>
Game guide page providing trait, item, augment, and champion reference data.
Page: Guide (/guide).
</purpose>

<routes>
- /guide      -> guide landing (default tab)
- /guide/:tab -> specific tab (traits / items / augments / champions)
</routes>

<api>
<backend>
- GET /api/guide         -> fetch public guide catalog
- GET /api/guide/{tab}   -> fetch public guide data for a specific tab
- GET /api/guide/{tab}?patchVersion=&query=&page=&pageSize=&sortKey=&sortDir=&cost=
- GET    /api/admin/guides?guideType=&patchVersion=&active= -> admin guide list
- POST   /api/admin/guides                                  -> create guide
- POST   /api/admin/guides/import/cdragon                   -> import champion/trait/item/augment guides from Community Dragon
- PATCH  /api/admin/guides/{guideId}                        -> update guide
- DELETE /api/admin/guides/{guideId}                        -> soft delete guide
</backend>
<frontend>
- frontend/src/api/guide.ts            -> main guide API calls
- frontend/src/api/guideClient.ts      -> HTTP client wrapper for guide
- frontend/src/api/guideFallback.ts    -> fallback data when API is unavailable
- frontend/src/api/guideNormalizers.ts -> normalize raw guide data before use
- frontend/src/api/guideTypes.ts       -> TypeScript types for guide domain
- frontend/src/hooks/useGuide.ts       -> TanStack Query hooks for guide catalog and tab pages
- frontend/src/pages/Guide/hooks/      -> page UI state, tab pagination, metric sorting, and dialog state
- frontend/src/pages/Guide/components/ -> page-specific guide panels/cards/controls
</frontend>
</api>

<business-rules>
- Guide covers: trait, item, augment, and champion guide data.
- Backend guideType enum values are TRAIT, ITEM, AUGMENT, CHAMPION.
- Public tab path values are traits, items, augments, champions. Invalid tabs throw GUIDE_INVALID_TAB.
- Public responses use ApiResponse&lt;List&lt;GuideEntryResponse&gt;&gt; for /api/guide and ApiResponse&lt;GuidePageResponse&lt;GuideEntryResponse&gt;&gt; for /api/guide/{tab}.
- GuideEntryResponse includes id, guideType, targetKey, name, summary, imageUrl, patchVersion, sortOrder, dataJson.
- dataJson must serialize as a JSON object, not a raw JSON string.
- Local DB smoke data lives in guides. guideType + targetKey + patchVersion identifies one guide entry.
- The guides table has a unique constraint on guideType + targetKey + patchVersion.
- v0.3.0 policy: soft-deleted guide rows still reserve guideType + targetKey + patchVersion.
- Admin create/import must not recreate a guide with the same guideType + targetKey + patchVersion when a soft-deleted row already exists; use skip or restore/reactivation behavior instead.
- Allowing same-key recreation after delete requires a separate DB migration/index policy change, such as a uniqueness rule scoped to non-deleted rows, plus matching service duplicate checks.
- If patchVersion is omitted, public APIs resolve the latest active non-deleted patch version. Do not mix multiple patch versions in one public response.
- Latest patch selection sorts patchVersion numerically by major/minor parts, then lexicographically as a final tie-breaker.
- GET /api/guide returns all active, non-deleted rows for the latest active patch ordered by sortOrder ASC, id ASC.
- GET /api/guide/{tab} returns one active, non-deleted tab page for the resolved patch.
- Public page defaults are page=1 and pageSize=10. page must be 1..10000 and pageSize must be 1..100.
- Public sortKey is optional and limited to avgPlace, pickRate, top4, winRate. sortDir is optional and limited to asc or desc.
- Without sortKey, public tab pages sort by sortOrder ASC, id ASC.
- With sortKey, metric values are read from dataJson. Numeric values are used directly; text values parse the first number after whitespace removal and comma-to-dot normalization. Missing/unparseable metrics sort last, then fall back to sortOrder/id.
- cost filter is valid only as 1..5. Repository applies cost only when guideType is CHAMPION.
- query searches name, summary, and targetKey for the active patch/tab. Escape `%`, `_`, and `\` before DB LIKE search.
- If no active patch exists, /api/guide returns [] and /api/guide/{tab} returns an empty page with totalPages=1.
- Invalid persisted dataJson is treated as GUIDE_INVALID_DATA and should be fixed in admin data/import logic, not hidden by public API fallback.
- Admin endpoints are protected by X-Admin-Token through /api/admin/**.
- AdminGuideRequest uses guideType, targetKey, name, summary, imageUrl, dataJson, patchVersion, sortOrder, active.
- Admin writes validate dataJson as a JSON object, trim required text fields, require sortOrder >= 0, and default active to true on create when omitted.
- Admin responses include active, createdAt, updatedAt, and deletedAt in addition to the public guide fields.
- Admin delete uses soft delete through active/deletedAt. Do not hard delete guide rows.
- Admin CDragon import writes into the same guides contract as manual admin curation.
- CDragon import currently supports CHAMPION, TRAIT, ITEM, and AUGMENT guide rows.
- CDragon import is upsert-based for non-deleted rows: same guideType + targetKey + patchVersion updates existing rows while preserving active state, and creates missing rows as active.
- If a soft-deleted row already reserves the same key, CDragon import skips that key instead of recreating it.
- CDragon import item filtering includes only completed craftable TFT_Item_* rows with exactly two composition components and no associatedTraits.
- CDragon item import excludes component-only rows, emblem/trait-associated rows, radiant/artifact/support/non-craftable rows that do not match the completed-item policy.
- CDragon item import creates base static ITEM rows from CDragon item data and keeps display-safe metric fallbacks when cached match data is unavailable.
- Current develop may opportunistically enrich item metric fields and bestUsers from a bounded cached_match sample during CDragon import, but this is an MVP/QA enrichment path only, not the final production metric sourcing policy tracked by #393.
- CDragon augment import reads only the requested set/mutator augments and requires apiName, name, description, and icon.
- CDragon augment import excludes debug/dummy/test/placeholder/inactive/disabled entries by apiName/name/description keywords.
- CDragon augment import creates base static AUGMENT rows from the requested set/mutator and keeps display-safe metric fallbacks when cached match data is unavailable.
- Current develop may opportunistically enrich augment metric fields from a bounded cached_match sample during CDragon import, but this is an MVP/QA enrichment path only, not the final production metric sourcing policy tracked by #393.
- #393 guide metric strategy separates static guide import from performance metric refresh: CDragon provides names/descriptions/images/tags/rewards, while Riot match detail stored in cached_match provides avgPlace, pickRate, TOP4 rate, winRate, and sampleCount.
- Current metric source is existing cached_match data created by user match searches. This is acceptable for local QA and MVP verification, but it can be biased or too small because it only reflects searched users.
- Current metric enrichment reads no new Riot matches. It only samples already stored cached_match rows.
- Cached match guide metrics use queueId 1090 and 1100 only.
- Cached match guide metrics must read a bounded recent sample instead of all cached matches. The current cap is 500 matches, sorted by gameDatetime DESC and matchId DESC.
- The bounded cached match query requires a matching DB index on cached_match(queue_id, game_datetime DESC, match_id DESC) when applying production-like schema changes manually.
- #393 next implementation should introduce a CDragon-import-independent metric refresh path, such as an admin/manual refresh API or batch job, before treating guide metrics as production-quality service data.
- #393 later collection policy should decide whether to keep using only cached_match, add top-tier/statistics-oriented Riot match collection, or combine both. Raw match detail should remain stored in cached_match so changed formulas or bugs can be re-aggregated.
- Metric refresh must keep "-" and [] fallbacks when an item or augment is below the minimum sample size; do not invent numbers from insufficient samples.
- CDragon import request fields: patchVersion (required, max 20), setNumber (default 17), mutator (default TFTSet{setNumber}), includeChampions (default true), includeTraits (default true), includeItems (default false), includeAugments (default false).
- CDragon import rejects requests where includeChampions, includeTraits, includeItems, and includeAugments all resolve to false.
- CDragon import response fields: createdCount, updatedCount, skippedCount, championCount, traitCount, itemCount, augmentCount, importedCount (= createdCount + updatedCount).
- Data originates from CDragon (traits, champions) where possible; use communityDragonAssets.ts helpers for frontend images.
- guideFallback.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
- Public guide UI should request tab data through `useGuideTabItems`; components must not fetch guide data directly.
- Page-level state such as active tab, search, favorites, recent guides, pagination, and metric sort belongs in Guide page hooks.
- Page components should remain composition-focused. Repeated UI units such as champion cards, augment stat tables, reward panels, and planner panels belong under pages/Guide/components/.
</business-rules>

<data-contracts>
<guide-entry>
- guideType: TRAIT | ITEM | AUGMENT | CHAMPION
- targetKey: stable domain key such as CDragon apiName
- name: display name
- summary: short display text
- imageUrl: optional public image URL
- dataJson: tab-specific JSON object
- patchVersion: patch scope such as 17.3
- sortOrder: deterministic display order
- active: public visibility flag; admin-only field
- deletedAt: soft-delete marker; admin-only field
</guide-entry>

<cdragon-champion-data-json>
- cost: 1..5
- role: CDragon role text or fallback text
- position: imported position label
- traits: string[]
- bestItems: [] at current import stage
- stats: { ad, armor, attackSpeed, hp, mana, mr, range }
- ability: { name, description, iconUrl }
</cdragon-champion-data-json>

<cdragon-trait-data-json>
- count: max minUnits from CDragon effects
- type: synergy label
- summary: sanitized CDragon desc
- tone: bronze | silver | gold | prismatic, derived from max effect style
- levels: effect minUnits list, using "N+" when maxUnits is open-ended
- tips: [] at current import stage
- champions: [{ cost, imageUrl, name }]
</cdragon-trait-data-json>

<cdragon-item-data-json>
- category: "완성 아이템"
- description: sanitized CDragon desc
- avgPlace: cached match average placement or "-"
- pickRate: cached match pick rate or "-"
- top4: cached match TOP4 rate or "-"
- winRate: cached match win rate or "-"
- sampleCount: not persisted by the current CDragon import; add this with the #393 metric refresh contract before presenting production-quality guide metrics
- bestUsers: top cached match users or []
- combinations: [{ label: "조합식", note: "CDragon 조합 기준", items: [{ imageUrl, name }] }]
</cdragon-item-data-json>

<cdragon-augment-data-json>
- description: sanitized CDragon desc/description/tooltip
- tags: displayable CDragon tags plus description-derived tags, or ["공용"]
</cdragon-augment-data-json>
</data-contracts>

<backend-implementation>
- GuideController owns public read endpoints under /api/guide.
- GuideServiceImpl resolves latest patch, validates query params, parses dataJson, sorts metrics, and builds GuidePageResponse.
- AdminGuideController owns admin CRUD and /api/admin/guides/import/cdragon.
- AdminGuideServiceImpl owns admin validation, duplicate prevention, dataJson serialization, and soft delete.
- GuideCdragonImportServiceImpl fetches CommunityDragonProperties.tftKoKrUrl using RestTemplate and builds guide candidates.
- GuideCdragonImportServiceImpl currently enriches ITEM and AUGMENT guide candidates with cached match metrics when matching patch data exists. Treat this as import-time MVP enrichment, not a dedicated metric refresh service.
- GuideCdragonImportServiceImpl reads recent cached matches through CachedMatchRepository.findRecentByQueueIds with PageRequest.of(0, 500).
- CachedMatchRepository.findRecentByQueueIds filters by queueId and orders by gameDatetime DESC, matchId DESC; keep the query and DB index aligned.
- No separate GuideMetricRefreshService, admin refresh endpoint, scheduler, or top-tier Riot match collector exists in current develop. Add those in a separate #393 implementation PR instead of expanding the CDragon import contract further.
- CDragon set data resolution first searches root.setData by setNumber + mutator, then falls back to root.sets[setNumber] if champions and traits exist.
- Champion import includes only shop champions whose apiName starts with TFT{setNumber}_, cost is 1..5, and name is present.
- Import asset URLs use CommunityDragonProperties.assetBaseUrl plus a lowercased asset path with .tex replaced by .png.
- Import sanitization removes HTML tags, removes @placeholder@ tokens, collapses whitespace, and trims text.
- Concurrent import create conflicts are retried as update when a non-deleted row appears, or skipped when only a reserved/soft-deleted key exists.
</backend-implementation>

<backend-structure>
- Controller: backend/src/main/java/com/tftgogo/domain/guide/controller/
- Swagger docs: backend/src/main/java/com/tftgogo/domain/guide/controller/docs/
- Request DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/guide/service/ and service/impl/
- CDragon config: backend/src/main/java/com/tftgogo/global/cdragon/config/CommunityDragonProperties.java
- Repository: backend/src/main/java/com/tftgogo/domain/guide/repository/GuideRepository.java
- Entity: backend/src/main/java/com/tftgogo/domain/guide/entity/Guide.java
</backend-structure>

<validation>
- Service-layer unit tests are the primary backend verification target.
- Admin guide tests should cover list filtering, create, update, duplicate prevention, invalid dataJson, not found, and soft delete.
- CDragon import tests should cover create, update, active state preservation, skipped soft-deleted key behavior, and missing set/mutator input.
- CDragon import tests should verify that cached match stats use page=0 and pageSize=500 when item or augment metrics are requested.
- #393 metric refresh tests should be added with the dedicated refresh implementation: cached match fixture aggregation, queue/patch filtering, duplicate match handling, minimum sample fallback, sampleCount, and idempotent guide dataJson updates.
- Public guide tests should continue to cover tab parsing, page/pageSize bounds, sortKey/sortDir validation, cost filtering, dataJson object response, metric sorting, latest patch fallback, empty latest patch behavior, and LIKE escaping.
- Import tests should keep asserting importedCount semantics through createdCount + updatedCount, not championCount + traitCount.
- Frontend guide tests should cover asset helper behavior when set-specific champion, trait, or item paths are derived from shared config.
</validation>

<data-ingestion>
- Current stage: CDragon champion/trait/item/augment guide import plus import-time cached_match metric enrichment for item/augment rows.
- Current match-stat stage intentionally uses a recent bounded sample. This verifies the calculation/display path, but does not solve the production data quality question from #393.
- Next #393 stage is to decide and implement the metric data source/collection policy, then move metric recomputation into a separate refresh path from CDragon static import.
- After #393, broader guide quality work can expand match collection by tier/patch/queue/region and increase sample size beyond local QA defaults.
- AI server/FastAPI is not required for guide CRUD. Add it only when AI/RAG/recommendation behavior needs guide data.
- Current CDragon import does not curate bestItems/tips and should not be treated as final editorial guide quality.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/Guide/
</frontend-structure>

</spec>
