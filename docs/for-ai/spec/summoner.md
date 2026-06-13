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
- GET /api/summoners/{gameName}/{tagLine}
  → SummonerDetailResponse { puuid, gameName, tagLine, profileIconId, summonerLevel, tier, rank, leaguePoints, wins, losses }
  — 매치 데이터 미포함. unranked 시 tier/rank=null, leaguePoints/wins/losses=0

- GET /api/match/{puuid}/matches?start={int}&count={int}
  → List&lt;SummonerMatchItemDto&gt; { matchId, placement, gameDateTime, gameType, compositionName,
      traits: [ { traitId, name, iconUrl, count, tone(bronze|silver|gold|prismatic) } ],
      units:  [ { characterId, imageUrl, stars, itemImageUrls } ],
      participants: [ { puuid, riotIdGameName, riotIdTagline, placement, stage, traits, units, playersEliminated, goldLeft } ] }
  — start 기본값 0, count 기본값 20. Riot API 호출 간 1,300ms 쓰로틀 (RiotApiClient 단일 rate limiter). queue 1090·1100만 포함.

- GET /api/match/detail/{matchId}
  → MatchDetailResponse — 매치에 참가한 8인 전체 상세 데이터
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
- Match list: count 파라미터로 배치 크기 지정 (기본값 20). "더 보기" 버튼 클릭 시 start를 count 단위로 증가시켜 추가 요청. 내부적으로 matchId 조회 및 매치 상세 조회 모두 RiotApiClient의 단일 rate limiter(1,300ms)를 통해 직렬 처리.
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
- stage 필드: 현재 ParticipantDto.stage = String.valueOf(level) 임시값 — Spring round notation 변환 미구현
- gameType label 변환 정책 미확정 (queue_id → "랭크"/"일반" 등)
- SummonerMatchItemDto.compositionName 항상 빈 문자열 — 추후 구현 필요
- itemImageUrls 항상 빈 리스트 — Riot API 아이템 ID → 이미지 URL 변환 미구현
</open-issues>

</spec>
