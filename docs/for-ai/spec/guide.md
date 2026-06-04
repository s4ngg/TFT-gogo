<spec domain="guide">

<purpose>
Game guide page providing unit, synergy, and item reference data.
Page: Guide (/guide).
</purpose>

<routes>
- /guide         → guide landing (default tab)
- /guide/:tab    → specific tab (traits / items / augments / champions)
</routes>

<api>
<backend>
- GET /api/guide         — fetch all guide data (default tab)
- GET /api/guide/{tab}   — fetch guide data for a specific tab
- GET /api/guide/{tab}?patchVersion=&query=&page=&pageSize=&sortKey=&sortDir=&cost=
- Admin draft: /api/admin/guides
</backend>
<frontend>
- frontend/src/api/guide.ts            — main guide API calls
- frontend/src/api/guideClient.ts      — HTTP client wrapper for guide
- frontend/src/api/guideFallback.ts    — fallback data when API is unavailable
- frontend/src/api/guideNormalizers.ts — normalize raw guide data before use
- frontend/src/api/guideTypes.ts       — TypeScript types for guide domain
</frontend>
</api>

<business-rules>
- Guide covers: trait, item, augment, and champion guide data.
- Backend guideType enum values are TRAIT, ITEM, AUGMENT, CHAMPION.
- Public responses use ApiResponse&lt;List&lt;GuideEntryResponse&gt;&gt; for /api/guide and ApiResponse&lt;GuidePageResponse&lt;GuideEntryResponse&gt;&gt; for /api/guide/{tab}.
- GuideEntryResponse includes id, guideType, targetKey, name, summary, imageUrl, patchVersion, sortOrder, dataJson.
- dataJson must serialize as a JSON object, not a raw JSON string.
- Local DB smoke data lives in guides. guideType + targetKey + patchVersion identifies one guide entry.
- If patchVersion is omitted, do not mix multiple patch versions in one public response.
- Escape `%`, `_`, and `\` in query before DB LIKE search.
- Admin writes validate dataJson as a JSON object and use soft delete through isActive/deletedAt.
- Data originates from CDragon (traits, champions) where possible — use communityDragonAssets.ts helpers for images.
- guideFallback.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
</business-rules>

<frontend-structure>
- frontend/src/pages/Guide/
</frontend-structure>

</spec>
