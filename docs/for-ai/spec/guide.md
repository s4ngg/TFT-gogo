<spec domain="guide">

<purpose>
Game guide page providing unit, synergy, and item reference data.
Page: Guide (/guide).
</purpose>

<routes>
- /guide         → guide landing (default tab)
- /guide/:tab    → specific tab (champions / synergies / items)
</routes>

<api>
<backend>
- GET /api/guide         — fetch all guide data (default tab)
- GET /api/guide/{tab}   — fetch guide data for a specific tab
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
- Guide covers: champion stats, synergy descriptions, item effects.
- Data originates from CDragon (traits, champions) — use communityDragonAssets.ts helpers for images.
- guideFallback.ts provides static fallback when the backend is unreachable.
- guideNormalizers.ts must be applied before passing data to components; do not use raw API responses directly.
</business-rules>

<frontend-structure>
- frontend/src/pages/Guide/
</frontend-structure>

</spec>
