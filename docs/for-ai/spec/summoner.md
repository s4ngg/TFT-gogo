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
  — start 기본값 0, count 기본값 20. queue 1100(랭크)·1090(일반) 순차 조회 후 LinkedHashSet 병합 (랭크 순서 우선). 모든 Riot API 호출은 RiotRateLimiter(단기 20req/1s · 장기 100req/2min 이중 토큰 버킷)를 통해 rate limit 적용.

- GET /api/match/{puuid}/stats
  → PlayerStatsResponse { topTraits: [...], topChampions: [...] }
  — DB 캐시 전체 매치 집계. 많이 플레이한 시너지/챔피언 TOP 5. 더 보기와 무관한 독립 호출.

- GET /api/match/detail/{matchId}
  → MatchDetailResponse — 매치에 참가한 8인 전체 상세 데이터

- POST /api/summoners/{gameName}/{tagLine}/refresh
  → SummonerProfileResponse — Riot API에서 최신 데이터를 가져와 캐시 갱신. 타임아웃 90초.
  — 429 응답 시 `Retry-After` 헤더 포함 (초 단위). 기본값 120초.
</backend>
<frontend>
- frontend/src/api/summonerApi.ts               — getSummonerProfile, getMatchHistory
- frontend/src/api/communityDragonAssets.ts     — tftChampSquareUrl, tftTraitIconUrl (CDragon CDN)
- frontend/src/hooks/useMatchHistory.ts         — getMatchHistory 래핑 + 페이징 상태 관리
- frontend/src/hooks/useSummonerProfile.ts      — getSummonerProfile 래핑
- frontend/src/pages/SummonerDetail/SummonerDetail.tsx
</frontend>
</api>

<business-rules>
- Win = placement ≤ 4. Loss = placement > 4. Never use Riot API `win` field.
- LeagueEntryDTO wins/losses are Riot's all-time totals — do NOT use for win rate calculation.
- 많이 플레이한 시너지/챔피언 집계: 현재 시즌 전체 매치 기록 기반. 더 보기로 불러온 목록과 무관하게 고정값으로 표시. 별도 시즌 전체 매치 조회가 필요하다.
- 게임 요약(순방확률·평균순위): 현재까지 불러온 전체 매치 기준. 더 보기 클릭 시 재집계. 30게임으로 제한하지 않는다.
- queue_id 1100 = Ranked, 1090 = Normal. Exclude all other queue types.
- Champion star level: use Riot API `tier` (int) directly.
- Trait activation tone: Riot API `style` (0–4) → 0=none, 1=bronze, 2=silver, 3=gold, 4=chromatic.
- LP change and augments are not provided by Riot API — never fabricate these values.
- CDragon image fallback: if registered URL missing, auto-generate `Trait_Icon_17_{TraitName}.TFT_Set17.tex` pattern.
- Match list: count 파라미터로 배치 크기 지정 (기본값 20). "더 보기" 버튼 클릭 시 start를 count 단위로 증가. getNextPageParam 조건: lastPage.length > 0이면 다음 페이지 존재로 간주. 모든 Riot API 호출은 백엔드 RiotRateLimiter가 rate limit 보장.
- Game type filter: 전체 / 랭크 / 일반 — applied client-side on fetched match list.
- Expanding a match row shows all 8 participants: placement, summonerName, stage (last_round → Spring notation), traits, units, kills, gold_left.
- My row in expanded view is highlighted in teal.
- stage conversion: last_round integer → Spring round notation (e.g., 5 → 2-1).
- Non-existent summoner search → show empty state message.
- Search input: split('#') 후 gameName·tagLine 양쪽 모두 trim(). "닉네임 # KR1" 형태 입력 허용.
- 429 rate limit:
  - 프로필 조회 또는 전적 갱신(POST /refresh) 429 → RateLimitState 컴포넌트 표시. Retry-After 초 카운트다운 (기본값 120초, 0이 되면 "다시 검색할 수 있습니다" 표시).
  - 전적 목록 조회(GET /api/match/{puuid}/matches) 429 → matchRateLimited 플래그 → 목록 하단 에러 메시지 표시 ("전적 갱신에 실패했습니다. 잠시 후 다시 시도해주세요."). Retry-After 카운트다운 없음.
</business-rules>

<frontend-structure>
- frontend/src/pages/SummonerDetail/SummonerDetail.tsx              — main page component
- frontend/src/pages/SummonerDetail/components/RecentSummary.tsx    — 게임 요약 카드 (불러온 전체 매치 기준)
- frontend/src/pages/SummonerDetail/components/MatchDetailPanel.tsx — 전적 행 상세 펼침 패널 (8인)
- frontend/src/pages/SummonerDetail/components/EmptyState.tsx       — 소환사 없음 빈 상태
- frontend/src/pages/SummonerDetail/utils/summonerUtils.ts          — timeAgo, formatDate, placementTone, detailRankClass
- frontend/src/hooks/useMatchHistory.ts                             — 전적 목록 fetching hook
- frontend/src/hooks/useSummonerProfile.ts                          — 프로필 fetching hook
- frontend/src/api/summonerApi.ts                                   — API calls
- frontend/src/api/communityDragonAssets.ts                         — CDragon image URL helpers
</frontend-structure>

<open-issues>
- stage 필드: 현재 ParticipantDto.stage = String.valueOf(level) 임시값 — Spring round notation 변환 미구현
- gameType label 변환 정책 미확정 (queue_id → "랭크"/"일반" 등)
- SummonerMatchItemDto.compositionName 항상 빈 문자열 — 추후 구현 필요
- itemImageUrls 항상 빈 리스트 — Riot API 아이템 ID → 이미지 URL 변환 미구현
</open-issues>

</spec>
