<spec domain="dashboard">

<purpose>
Summoner search and meta snapshot features.
Pages: Dashboard (/), MetaStats.
</purpose>

<routes>
- /                          → Dashboard (search box + meta snapshot)
</routes>

<api>
<backend>
- 검색 자체는 별도 API 없음. 입력된 gameName#tagLine을 /summoner/:gameName/:tagLine으로 프론트엔드 라우팅.
- 소환사 데이터 조회: GET /api/summoners/{gameName}/{tagLine} (summoner.md 참조)
</backend>
<frontend>
- frontend/src/api/summonerApi.ts — getSummonerProfile (검색 결과 페이지에서 사용)
</frontend>
</api>

<business-rules>
- Search format: gameName#tagLine (e.g., Hide on Bush#KR1).
- Meta snapshot is shown on Dashboard even without a search (default state).
- Meta snapshot tabs: 종합 / 상위권 / 마스터+ — each filters the displayed deck list.
- Recent searches: localStorage key `recentSearches`, max 5 entries, latest first. Shown in TopSummaryCards only when search input is focused. Reflects the user's own search history — not a fixed popular list.
</business-rules>

<frontend-structure>
- frontend/src/pages/Dashboard/         — main search page
- frontend/src/pages/MetaStats/         — meta snapshot (may be embedded in Dashboard)
</frontend-structure>

</spec>
