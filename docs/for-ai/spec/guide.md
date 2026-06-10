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
- The guides table has a unique constraint on guideType + targetKey + patchVersion. Soft-deleted rows still reserve that key unless the schema changes.
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
- CDragon item import stores statistic fields as "-" and bestUsers as [] until match/stat aggregation is connected.
- CDragon augment import reads only the requested set/mutator augments and requires apiName, name, description, and icon.
- CDragon augment import excludes debug/dummy/test/placeholder/inactive/disabled entries by apiName/name/description keywords.
- CDragon augment import stores statistic fields as "-" until match/stat aggregation is connected.
- CDragon import request fields: patchVersion (required, max 20), setNumber (default 17), mutator (default TFTSet{setNumber}), includeChampions (default true), includeTraits (default true), includeItems (default false), includeAugments (default false).
- CDragon import rejects requests where includeChampions, includeTraits, includeItems, and includeAugments all resolve to false.
- CDragon import response fields: createdCount, updatedCount, skippedCount, championCount, traitCount, itemCount, augmentCount, importedCount (= createdCount + updatedCount).
- Data originates from CDragon (traits, champions) where possible; use communityDragonAssets.ts helpers for frontend images.
- guideFallback.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
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
- avgPlace: "-" until statistics aggregation is connected
- pickRate: "-" until statistics aggregation is connected
- top4: "-" until statistics aggregation is connected
- winRate: "-" until statistics aggregation is connected
- bestUsers: [] until statistics aggregation is connected
- combinations: [{ label: "조합식", note: "CDragon 조합 기준", items: [{ imageUrl, name }] }]
</cdragon-item-data-json>

<cdragon-augment-data-json>
- description: sanitized CDragon desc/description/tooltip
- type: CDragon augmentType/type/category or "공용"
- tier: S | A | B | C | D, derived from CDragon rarity/tier when available
- tags: CDragon tags or ["CDragon"]
- reward: "-"
- avgPlace: "-" until statistics aggregation is connected
- pickRate: "-" until statistics aggregation is connected
- winRate: "-" until statistics aggregation is connected
</cdragon-augment-data-json>
</data-contracts>

<backend-implementation>
- GuideController owns public read endpoints under /api/guide.
- GuideServiceImpl resolves latest patch, validates query params, parses dataJson, sorts metrics, and builds GuidePageResponse.
- AdminGuideController owns admin CRUD and /api/admin/guides/import/cdragon.
- AdminGuideServiceImpl owns admin validation, duplicate prevention, dataJson serialization, and soft delete.
- GuideCdragonImportServiceImpl fetches CommunityDragonProperties.tftKoKrUrl using RestTemplate and builds guide candidates.
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
- Public guide tests should continue to cover tab parsing, page/pageSize bounds, sortKey/sortDir validation, cost filtering, dataJson object response, metric sorting, latest patch fallback, empty latest patch behavior, and LIKE escaping.
- Import tests should keep asserting importedCount semantics through createdCount + updatedCount, not championCount + traitCount.
</validation>

<data-ingestion>
- Current stage: CDragon champion/trait/item/augment guide import after admin CRUD and public query contracts are stable.
- Next stage: patch-note crawling/import and match/stat aggregation connection.
- AI server/FastAPI is not required for guide CRUD. Add it only when AI/RAG/recommendation behavior needs guide data.
- Current CDragon import does not curate bestItems/tips and should not be treated as final editorial guide quality.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/Guide/
</frontend-structure>

</spec>
