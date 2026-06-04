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
- GET /api/match/search?gameName=&tagLine=        — search summoner by name/tag (redirects to /summoner/:gameName/:tagLine)
</backend>
<frontend>
- frontend/src/api/riotApi.ts     — Riot API proxy wrappers
</frontend>
</api>

<business-rules>
- Search format: gameName#tagLine (e.g., Hide on Bush#KR1).
- Meta snapshot is shown on Dashboard even without a search (default state).
- Meta snapshot tabs: 종합 / 상위권 / 마스터+ — each filters the displayed deck list.
</business-rules>

<frontend-structure>
- frontend/src/pages/Dashboard/         — main search page
- frontend/src/pages/MetaStats/         — meta snapshot (may be embedded in Dashboard)
</frontend-structure>

</spec>
