<spec domain="common">

<purpose>
Loaded alongside every feature spec file. Defines shared utilities, asset helpers, and cross-cutting rules.
</purpose>

<assets>

<cdragon>
- All champion/trait/item images must be fetched via helpers in frontend/src/api/communityDragonAssets.ts.
- Do NOT hardcode CDragon CDN URLs inside components or API files.
- Locale handling is in frontend/src/api/cdragonLocale.ts.
</cdragon>

<riot-api-proxy>
- The backend proxies all Riot API calls. Frontend never calls Riot API directly.
- Riot API rate limit: 100 req / 2 min (Dev key). Design accordingly.
- frontend/src/api/riotApi.ts wraps the proxy endpoints.
</riot-api-proxy>

</assets>

<http>
- All frontend HTTP calls go through frontend/src/api/axiosInstance.ts.
- Never import axios or fetch directly inside components or pages.
- API layer lives in frontend/src/api/; keep business logic out of API files.
- Backend response envelope: ApiResponse&lt;T&gt; — unwrap .data before using in components.
</http>

<data-types>
- gameName + tagLine together identify a summoner (not summonerId alone).
- patch version format: major.minor string values such as "17.3" are used as keys across deck, guide, and patch-note domains.
- CDragon trait data is used for synergy calculation; do not re-implement synergy logic in components.
</data-types>

</spec>
