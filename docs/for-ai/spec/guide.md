<spec domain="guide">

<purpose>
Game Guide provides public TFT reference data for traits, items, augments, and champions.
Page: /guide.
</purpose>

<routes>
- /guide -> single public guide page. The active tab is local UI state, not a route param.
</routes>

<api>
<backend>
- GET /api/guide
  -> fetch public guide catalog for the resolved latest patch.
- GET /api/guide/{tab}?patchVersion=&query=&page=&pageSize=&sortKey=&sortDir=&cost=
  -> fetch one public guide tab page.
- GET /api/admin/guides?guideType=&patchVersion=&active=
  -> legacy/manual guide curation list from guides.
- POST /api/admin/guides
  -> create one legacy/manual guide row.
- PATCH /api/admin/guides/{guideId}
  -> update one legacy/manual guide row.
- DELETE /api/admin/guides/{guideId}
  -> soft-delete one legacy/manual guide row.
- POST /api/admin/guides/import/cdragon
  -> import split guide tables from Community Dragon.
</backend>
<frontend>
- frontend/src/api/guide.ts -> public exports.
- frontend/src/api/guideClient.ts -> guide HTTP client functions.
- frontend/src/api/guideTypes.ts -> guide types, tab metadata, and page-size constants.
- frontend/src/api/guideNormalizers.ts -> raw API normalization.
- frontend/src/api/guideFallback.ts -> fallback data when backend is unavailable.
- frontend/src/hooks/useGuide.ts -> TanStack Query hooks.
- frontend/src/pages/Guide/hooks/ -> guide UI state and pagination.
- frontend/src/pages/Guide/components/ -> guide tab panels, cards, controls, and dialogs.
</frontend>
</api>

<current-data-model>
- Current public guide data is split by domain tables:
  - tft_guide_champions
  - tft_guide_traits
  - tft_guide_items
  - tft_guide_augments
  - augment_guide_plans
- guides still exists for legacy/manual admin curation and as a fallback source when split-table data is absent.
- Public read flow prefers split tables. It falls back to guides only when the resolved tab has no split-table rows.
- CDragon import writes to split tables, not to guides.
- Manual admin CRUD still writes to guides.
- Do not remove guides until admin/manual curation is migrated or explicitly deprecated.
</current-data-model>

<erd-contract>
<tft_guide_champions>
- id: BIGINT AUTO_INCREMENT PRIMARY KEY
- champion_key: VARCHAR(100) NOT NULL
- name: VARCHAR(100) NOT NULL
- cost: TINYINT NOT NULL
- role: VARCHAR(50) NOT NULL
- position: VARCHAR(50) NOT NULL
- image_url: VARCHAR(500) NOT NULL
- stats_json: JSON NOT NULL
- traits_json: JSON NOT NULL
- best_items_json: JSON NOT NULL
- patch_version: VARCHAR(20) NOT NULL
- created_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
- updated_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
- UNIQUE (champion_key, patch_version)
- INDEX (patch_version, cost, id)
</tft_guide_champions>

<tft_guide_traits>
- id: BIGINT AUTO_INCREMENT PRIMARY KEY
- trait_key: VARCHAR(100) NOT NULL
- name: VARCHAR(100) NOT NULL
- type: VARCHAR(50) NOT NULL
- icon_url: VARCHAR(500) NOT NULL
- tone: VARCHAR(30) NOT NULL
- summary: TEXT NOT NULL
- levels_json: JSON NOT NULL
- tier_effects_json: JSON NOT NULL
- champions_json: JSON NOT NULL
- special_units_json: JSON NOT NULL
- tips_json: JSON NOT NULL
- patch_version: VARCHAR(20) NOT NULL
- created_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
- updated_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
- UNIQUE (trait_key, patch_version)
- INDEX (patch_version, id)
</tft_guide_traits>

<tft_guide_items>
- id: BIGINT AUTO_INCREMENT PRIMARY KEY
- item_key: VARCHAR(100) NOT NULL
- name: VARCHAR(100) NOT NULL
- category: VARCHAR(50) NOT NULL
- image_url: VARCHAR(500) NOT NULL
- description: TEXT NULL
- stats_json: JSON NOT NULL
- best_users_json: JSON NOT NULL
- combinations_json: JSON NOT NULL
- patch_version: VARCHAR(20) NOT NULL
- created_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
- updated_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
- UNIQUE (item_key, patch_version)
- INDEX (patch_version, id)
</tft_guide_items>

<tft_guide_augments>
- id: BIGINT AUTO_INCREMENT PRIMARY KEY
- augment_key: VARCHAR(100) NOT NULL
- name: VARCHAR(100) NOT NULL
- description: TEXT NOT NULL
- icon_url: VARCHAR(500) NULL
- tags_json: JSON NOT NULL
- stats_json: JSON NOT NULL
- patch_version: VARCHAR(20) NOT NULL
- created_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
- updated_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
- UNIQUE (augment_key, patch_version)
- INDEX (patch_version, name, id)
</tft_guide_augments>

<augment_guide_plans>
- id: BIGINT AUTO_INCREMENT PRIMARY KEY
- plan_key: VARCHAR(50) NOT NULL
- label: VARCHAR(100) NOT NULL
- stages_json: JSON NOT NULL
- patch_version: VARCHAR(20) NOT NULL
- created_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
- updated_at: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
- UNIQUE (plan_key, patch_version)
- INDEX (patch_version, id)
</augment_guide_plans>

<removed-or-deprecated>
- augment_guide_rewards is removed.
- tft_guide_augments.tier is removed.
- tft_guide_augments.type is removed.
- tft_guide_augments.reward is removed.
</removed-or-deprecated>
</erd-contract>

<public-behavior>
- Public tabs are traits, items, augments, and champions. Invalid tabs throw GUIDE_INVALID_TAB.
- GET /api/guide returns ApiResponse<GuideCatalogResponse>.
- GuideCatalogResponse contains patchVersion, entries, and augmentPlans.
- GET /api/guide/{tab} returns ApiResponse<GuidePageResponse<GuideEntryResponse>>.
- GuideEntryResponse contains id, guideType, targetKey, name, summary, imageUrl, patchVersion, sortOrder, and dataJson.
- dataJson must serialize as a JSON object, not a raw JSON string.
- If patchVersion is omitted, public APIs resolve the latest patch across split tables and legacy guides.
- Latest patch selection sorts patchVersion numerically by major/minor, suffix, then lexicographically.
- A public response must not mix multiple patch versions.
- Empty data returns an empty catalog or an empty page with totalPages=1.
- page defaults to 1 and must be 1..10000.
- Backend pageSize defaults to 10 and must be 1..100.
- Frontend current display defaults:
  - traits: 6 per page.
  - items: 6 per page.
  - augments: 6 per page.
  - champions: 15 per page.
- cost filter is valid only for champions and must be 1..5.
- query searches name, summary, and targetKey for the resolved tab.
- Split-table tab data sorts by:
  - champions: cost ASC, name ASC, id ASC.
  - traits: name ASC, id ASC.
  - items: name ASC, id ASC.
  - augments: name ASC, id ASC.
- sortKey support remains for legacy metric fields avgPlace, pickRate, top4, and winRate. Current public UI no longer depends on those metrics for item/augment display.
</public-behavior>

<data-json-contract>
<champion-data-json>
- cost: number
- role: string
- position: string
- stats: object
- traits: array
- bestItems: array
</champion-data-json>

<trait-data-json>
- count: max display level derived from levels_json.
- type: string.
- summary: string.
- tone: bronze | silver | gold | prismatic or imported tone.
- levels: string[].
- tierEffects: [{ level, description }].
- champions: [{ cost, imageUrl, name }].
- specialUnits: [{ imageUrl, name, note }].
- tips: string[].
- specialUnits is required for summon/generated trait units such as Dark Star's small black hole. Such units must not appear as normal champions.
</trait-data-json>

<item-data-json>
- category: string, retained for data completeness.
- description: string.
- bestUsers: array, retained for source data compatibility.
- combinations: array, retained for source data compatibility.
- Public UI currently shows item image, name, and description only. It intentionally hides category and combinations.
</item-data-json>

<augment-data-json>
- description: string.
- tags: string[].
- Public UI currently shows simplified augment cards and no longer shows tier, type, reward table, win rate, TOP4, avg place, or pick rate.
</augment-data-json>
</data-json-contract>

<import-rules>
- Admin CDragon import request fields:
  - patchVersion: required, max 20. The special value "latest" resolves to the current patch note version, then falls back to the latest non-deleted patch note.
  - setNumber: optional, default 17.
  - mutator: optional, default TFTSet{setNumber}.
  - includeChampions: optional, default true.
  - includeTraits: optional, default true.
  - includeItems: optional, request default false; app.guide.cdragon startup config default true.
  - includeAugments: optional, request default false; app.guide.cdragon startup config default true.
- Import rejects requests where all include flags resolve to false.
- Import response fields are createdCount, updatedCount, skippedCount, championCount, traitCount, itemCount, augmentCount, and importedCount.
- importedCount equals createdCount + updatedCount.
- Import is upsert-based by each split table's unique key.
- Import resolves CDragon set data by setNumber + mutator first, then falls back to root.sets[setNumber] when needed.
- Imported champion rows include shop champions only. Generated/summon units must be excluded from tft_guide_champions and attached to trait special_units_json when explicitly mapped.
- Current special unit mapping includes TFT17_DarkStar_FakeUnit as a special unit under TFT17_DarkStar.
- Asset URLs must be generated through backend TftAssetUrlBuilder/TftAssetConfig or frontend communityDragonAssets helpers. Do not hardcode CDragon URLs inside components.
- Verified asset overrides are allowed for known CDragon exceptions such as Rhaast and TFT17_DarkStar_FakeUnit.
- Import sanitization removes HTML tags, removes @placeholder@ tokens, collapses whitespace, and trims text.
</import-rules>

<scheduler>
- GuideCdragonImportScheduler exists for automatic guide import.
- Scheduler runs only on ApplicationReadyEvent and is controlled by app.guide.cdragon.startup-import.
- Startup import defaults are startup-import=false, patch-version=latest, set-number=17, mutator=TFTSet17, include-champions=true, include-traits=true, include-items=true, and include-augments=true.
- Scheduler code must remain config controlled so local/dev environments do not unintentionally crawl unless explicitly enabled.
- Manual admin import must remain available for local QA because automatic import can be disabled locally.
</scheduler>

<backend-structure>
- Public controller: backend/src/main/java/com/tftgogo/domain/guide/controller/GuideController.java
- Admin controller: backend/src/main/java/com/tftgogo/domain/guide/controller/AdminGuideController.java
- Services: backend/src/main/java/com/tftgogo/domain/guide/service/
- Implementations: backend/src/main/java/com/tftgogo/domain/guide/service/impl/
- Scheduler: backend/src/main/java/com/tftgogo/domain/guide/scheduler/GuideCdragonImportScheduler.java
- Split entities: GuideChampion, GuideTrait, GuideItem, GuideAugment, AugmentGuidePlan
- Legacy/manual entity: Guide
- Split repositories: GuideChampionRepository, GuideTraitRepository, GuideItemRepository, GuideAugmentRepository, AugmentGuidePlanRepository
- Legacy/manual repository: GuideRepository
</backend-structure>

<frontend-rules>
- Public Guide page must call API functions through frontend/src/api/guideClient.ts and hooks in frontend/src/hooks/useGuide.ts.
- Components must not import axios or fetch directly.
- guideNormalizers.ts must normalize raw API payloads before component use.
- Page-level state such as active tab, search, favorites, recent guides, and pagination belongs in Guide hooks.
- Item tab should not show "classification", "completed item", or "combination" UI labels.
- Augment tab should show 6 items per page and should not show tier/type/reward-table UI.
- Trait tab should show specialUnits separately from normal champions.
</frontend-rules>

<validation>
- Backend service tests should cover latest patch resolution, split-table priority over guides, empty data behavior, tab validation, pagination bounds, cost filtering, search, and invalid JSON handling.
- Import tests should cover create/update counts, importedCount semantics, item/augment include flags, generated/special unit exclusion from champions, trait special_units_json output, and asset overrides.
- Frontend tests should cover guide normalization, page-size expectations, item/augment simplified display, and trait special unit display.
- Browser QA should verify /guide on desktop and mobile has no page-level horizontal overflow and starts on the latest patch.
</validation>

</spec>
