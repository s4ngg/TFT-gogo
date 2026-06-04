<spec domain="dashboard">

<purpose>
Summoner search, match history, and meta snapshot features.
Pages: Dashboard (/), MetaStats, SummonerDetail (/summoner/:gameName/:tagLine).
</purpose>

<routes>
- /                          → Dashboard (search box + meta snapshot)
- /summoner/:gameName/:tagLine → SummonerDetail (profile + match history)
</routes>

<api>
<backend>
- GET /api/summoners/{gameName}/{tagLine}         — fetch summoner profile (tier, LP, win/loss)
- GET /api/summoners/{gameName}/{tagLine}/matches — fetch match list for summoner
- GET /api/match/search?gameName=&tagLine=        — search summoner by name/tag
- GET /api/match/{puuid}/matches                 — fetch matches by puuid
- GET /api/match/detail/{matchId}                — fetch single match detail
</backend>
<frontend>
- frontend/src/api/summonerApi.ts — summoner profile and match list calls
- frontend/src/api/riotApi.ts     — Riot API proxy wrappers
</frontend>
</api>

<business-rules>
- Search format: gameName#tagLine (e.g., Hide on Bush#KR1).
- Meta snapshot is shown on Dashboard even without a search (default state).
- Meta snapshot tabs: 종합 / 상위권 / 마스터+ — each filters the displayed deck list.
- Summoner card shows: gameName, tagLine, tier, LP, wins, losses, win rate.
- Match history shows per-game placement and unit composition.
</business-rules>

<frontend-structure>
- frontend/src/pages/Dashboard/         — main search page
- frontend/src/pages/SummonerDetail/    — summoner profile + match history
- frontend/src/pages/MetaStats/         — meta snapshot (may be embedded in Dashboard)
</frontend-structure>

</spec>
