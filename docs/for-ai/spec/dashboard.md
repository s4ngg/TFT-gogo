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
- 대시보드 검색 제출 시 소환사 프로필을 먼저 조회한다.
- 소환사 데이터 조회: GET /api/summoners/{gameName}/{tagLine} (summoner.md 참조)
- 검색 결과 카드의 상세 보기 CTA 클릭 시 /summoner/:gameName/:tagLine으로 프론트엔드 라우팅한다.
</backend>
<frontend>
- frontend/src/api/searchApi.ts — getSummonerProfile (대시보드 검색 결과 카드와 검색 결과 페이지에서 사용)
</frontend>
</api>

<business-rules>
- Search format: gameName#tagLine (e.g., Hide on Bush#KR1).
- 태그가 생략된 검색어는 KR1을 기본 태그로 사용한다. 단, 빈 검색어 또는 `#` 양쪽 중 하나가 비어 있는 검색어는 API를 호출하지 않고 입력 오류 상태를 표시한다.
- Dashboard 검색 상태는 idle / empty input / loading / success / not found / rate limited / error를 구분한다.
- Dashboard 검색 성공 카드에는 summoner name, tag, tier, LP, wins, losses, win rate를 표시한다. tier/rank가 없으면 언랭크로 표시하고 wins+losses가 0이면 승률은 `-`로 표시한다.
- Dashboard에서 검색 결과 카드의 상세 보기 CTA를 누를 때만 /summoner/:gameName/:tagLine으로 이동한다.
- Meta snapshot is shown on Dashboard even without a search (default state).
- Meta snapshot tabs: 종합 / 상위권 / 마스터+ — each filters the displayed deck list.
- Recent searches: localStorage key `tft_recent_searches`, max 5 entries, latest first. Always visible below the search box on page load (no focus required). Reflects the user's own successful profile lookups — not a fixed popular list. Empty, failed, not found, or rate-limited searches are not saved.
</business-rules>

<frontend-structure>
- frontend/src/pages/Dashboard/         — main search page
- frontend/src/pages/MetaStats/         — meta snapshot (may be embedded in Dashboard)
</frontend-structure>

</spec>
