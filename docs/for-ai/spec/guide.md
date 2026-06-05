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
- Public responses use ApiResponse&lt;List&lt;GuideEntryResponse&gt;&gt; for /api/guide and ApiResponse&lt;GuidePageResponse&lt;GuideEntryResponse&gt;&gt; for /api/guide/{tab}.
- GuideEntryResponse includes id, guideType, targetKey, name, summary, imageUrl, patchVersion, sortOrder, dataJson.
- dataJson must serialize as a JSON object, not a raw JSON string.
- Local DB smoke data lives in guides. guideType + targetKey + patchVersion identifies one guide entry.
- The guides table has a unique constraint on guideType + targetKey + patchVersion. Soft-deleted rows still reserve that key unless the schema changes.
- If patchVersion is omitted, do not mix multiple patch versions in one public response.
- Escape `%`, `_`, and `\` in query before DB LIKE search.
- Admin endpoints are protected by X-Admin-Token through /api/admin/**.
- AdminGuideRequest uses guideType, targetKey, name, summary, imageUrl, dataJson, patchVersion, sortOrder, active.
- Admin writes validate dataJson as a JSON object, trim required text fields, require sortOrder >= 0, and default active to true on create when omitted.
- Admin responses include active, createdAt, updatedAt, and deletedAt in addition to the public guide fields.
- Admin delete uses soft delete through isActive/deletedAt. Do not hard delete guide rows.
- Data originates from CDragon (traits, champions) where possible; use communityDragonAssets.ts helpers for frontend images.
- guideFallback.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
</business-rules>

<backend-structure>
- Controller: backend/src/main/java/com/tftgogo/domain/guide/controller/
- Swagger docs: backend/src/main/java/com/tftgogo/domain/guide/controller/docs/
- Request DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/request/
- Response DTOs: backend/src/main/java/com/tftgogo/domain/guide/dto/response/
- Services: backend/src/main/java/com/tftgogo/domain/guide/service/ and service/impl/
- Repository: backend/src/main/java/com/tftgogo/domain/guide/repository/GuideRepository.java
- Entity: backend/src/main/java/com/tftgogo/domain/guide/entity/Guide.java
</backend-structure>

<validation>
- Service-layer unit tests are the primary backend verification target.
- Admin guide tests should cover list filtering, create, update, duplicate prevention, invalid dataJson, not found, and soft delete.
- Public guide tests should continue to cover tab parsing, pagination bounds, cost filtering, dataJson object response, metric sorting, latest patch fallback, and LIKE escaping.
</validation>

<data-ingestion>
- Current stage: curated DB/admin data first, then server smoke testing.
- Next stage: CDragon/Riot-derived guide import after admin CRUD and public query contracts are stable.
- AI server/FastAPI is not required for guide CRUD. Add it only when AI/RAG/recommendation behavior needs guide data.
</data-ingestion>

<frontend-structure>
- frontend/src/pages/Guide/
</frontend-structure>

</spec>
