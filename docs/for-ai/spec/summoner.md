<spec domain="summoner">

<purpose>
Summoner profile lookup and TFT match history display.
Page: SummonerDetail (/summoner/:gameName/:tagLine).
Detailed human spec: docs/for-humans/spec/summoner.md
</purpose>

<routes>
- /summoner/:gameName/:tagLine → SummonerDetail (profile + rank + match history)
</routes>

<api>
<backend>
- GET /api/summoners/{gameName}/{tagLine}         — summoner profile (profileIconId, level, tier, rank, LP, wins, losses)
- GET /api/summoners/{gameName}/{tagLine}/matches — match list (placement, units, traits, gameDatetime, gameType, queueId)
</backend>
<frontend>
- frontend/src/api/summonerApi.ts               — getSummonerProfile, getMatchHistory
- frontend/src/api/communityDragonAssets.ts     — tftChampSquareUrl, tftTraitIconUrl (CDragon CDN)
- frontend/src/pages/SummonerDetail/SummonerDetail.tsx
</frontend>
</api>

<business-rules>
- Win = placement ≤ 4. Loss = placement > 4. Never use Riot API `win` field.
- LeagueEntryDTO wins/losses are Riot's all-time totals — do NOT use for recent 30-game win rate calculation.
- queue_id 1100 = Ranked, 1090 = Normal. Exclude all other queue types.
- Champion star level: use Riot API `tier` (int) directly.
- Trait activation tone: Riot API `style` (0–4) → 0=none, 1=bronze, 2=silver, 3=gold, 4=chromatic.
- LP change and augments are not provided by Riot API — never fabricate these values.
- CDragon image fallback: if registered URL missing, auto-generate `Trait_Icon_17_{TraitName}.TFT_Set17.tex` pattern.
- Match list: load 30 at a time; "30개 더 보기" button appends next batch.
- Game type filter: 전체 / 랭크 / 일반 — applied client-side on fetched match list.
- Expanding a match row shows all 8 participants: placement, summonerName, stage (last_round → Spring notation), traits, units, kills, gold_left.
- My row in expanded view is highlighted in teal.
- stage conversion: last_round integer → Spring round notation (e.g., 5 → 2-1).
- Non-existent summoner search → show empty state message.
</business-rules>

<frontend-structure>
- frontend/src/pages/SummonerDetail/SummonerDetail.tsx — main page component
- frontend/src/api/summonerApi.ts                      — API calls
- frontend/src/api/communityDragonAssets.ts            — CDragon image URL helpers
</frontend-structure>

<open-issues>
- POST /api/summoners/{gameName}/{tagLine}/refresh (match refresh endpoint) — not yet implemented
- Duplicate Riot API calls when loading profile + matches simultaneously (rate limit risk)
- stage conversion policy not yet finalized
- gameType label conversion policy not yet finalized
</open-issues>

</spec>
