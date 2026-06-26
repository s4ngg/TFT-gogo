<spec domain="common">

<purpose>
Loaded alongside every feature spec file. Defines shared utilities, asset helpers, and cross-cutting rules.
</purpose>

<assets>

<cdragon>
- All champion/trait/item images must be fetched via helpers in frontend/src/api/communityDragonAssets.ts.
- Do NOT hardcode CDragon CDN URLs inside components or API files.
- Locale handling is in frontend/src/api/cdragonLocale.ts.
- Browser code must not call raw.communitydragon.org locale JSON directly. Use the backend proxy endpoint `/api/cdragon/tft/ko-kr` through `axiosInstance` to avoid CORS/runtime drift.
- Backend owns the CDragon locale fetch/cache through `TftAssetCacheService`. It eagerly loads the locale at startup when possible and lazily fetches it if the cache is empty.
- TFT set-specific values such as `tft_set17`, `TFT_Set17`, and trait icon set numbers must be derived from the shared asset helpers/config where possible.
- Backend CDragon URLs should use `TftAssetUrlBuilder` and `TftAssetConfig` rather than duplicating set tags or CDN base URLs in feature code.
- Frontend TFT asset defaults live in `TFT_ASSET_CONFIG` in frontend/src/api/communityDragonAssets.ts.
  Use helper functions such as `tftChampSquareUrl`, `tftTraitIconUrl`, `tftItemIconUrl`, and `communityDragonAssetUrl` instead of rebuilding URL strings in feature code.
- Backend TFT asset defaults live in backend/src/main/java/com/tftgogo/global/riot/config/TftAssetConfig.java.
  Use `setTag`, `setFileSuffix`, `setUnitIdPrefix`, and `TftAssetUrlBuilder` when constructing champion or trait URLs/IDs. For item asset URLs, use the frontend `tftItemIconUrl` helper unless a backend item-specific helper is introduced.
- Set-specific override maps are allowed only for verified Community Dragon exceptions, legacy trait icon paths, or temporary fallback/demo assets. Prefer deriving new season paths from the shared config.
</cdragon>

<riot-api-proxy>
- The backend proxies all Riot API calls. Frontend never calls Riot API directly.
- Riot API rate limit: 100 req / 2 min (Dev key), 20 req / 1 s. 백엔드 RiotRateLimiter(이중 토큰 버킷)가 모든 Riot API 호출을 중앙에서 제어한다. 프론트엔드는 429 응답을 받을 경우 Retry-After 헤더 기준으로 처리한다.
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
