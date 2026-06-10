# Summoner — 소환사 전적 검색

> 상세 API 스펙: `docs/for-humans/backend/match-history-api-spec.md`

## 개요

소환사명#태그(Riot ID)를 입력하면 프로필, 랭크 정보, 최근 매치 목록 및 상세를 확인할 수 있다.
로그인 없이 누구나 검색 및 조회할 수 있다.

---

## 소환사 카드

- 프로필 아이콘, 소환사명, 태그, 레벨
- 티어 / LP / 승수·패수 / 승률(순방확률) / 평균 순위 / TOP4율
- 순위 분포 바 차트: 1~8위별 게임 수 시각화 (TOP4 청록 / 5~8위 레드)
- 전적 업데이트 버튼
  - 갱신 중 로딩 상태 표시, 완료 후 전적 목록 자동 리페치
  - 연속 클릭 방지(disabled) 처리

---

## 많이 플레이한 시너지 / 챔피언

- 시너지·챔피언별 게임 수 · 평균 등수 통계 목록
- Community Dragon CDN 이미지 연동

---

## 최근 30게임 요약

- 순방확률 도넛 차트 (TOP4 비율, CSS `conic-gradient`)
- 순방확률: `20W 10L (66.7%)` 형식 (4위 이상 = Win, 5~8위 = Loss)
- 평균 순위: `3.6th / 8`

---

## 전적 목록

- 순위 `1위`~`8위` 형식 + 순위별 색상 (1위 금색 / 2~4위 청록 / 5~8위 레드)
- 덱 이름 (Spring 서버에서 시너지/유닛 조합 자동 생성)
- 경과 시간 표시. LP 변동값은 Riot API 미제공으로 표시하지 않음
- 유닛 이미지 (CDN 기반, 3성 골드 테두리), 아이템 이미지 (챔피언당 최대 3개)
- 챔피언·아이템 이미지 호버 시 이름 툴팁 (`[data-tip]::after` CSS 방식)
- 게임 유형 배지 (랭크 / 일반) — queue_id 기준 (1100=랭크, 1090=일반)
- 게임 유형 필터: 전체 / 랭크 / 일반
- 30개씩 표시, "30개 더 보기" 버튼으로 추가 로드 (마지막 배치에 결과가 1개 이상이면 버튼 유지)
- 존재하지 않는 소환사 검색 시 빈 상태(Empty State) 안내 표시
- 검색 입력 시 `#` 기준으로 분리 후 gameName·tagLine 양쪽 공백 trim (예: "닉네임 # KR1" 입력 허용)
- Riot API 429 과호출 시:
  - 프로필 조회·전적 갱신: RateLimitState 표시 — Retry-After 초 카운트다운 (기본 120초)
  - 전적 목록 조회: 목록 하단 에러 메시지 표시 ("전적 갱신에 실패했습니다. 잠시 후 다시 시도해주세요.")

---

## 전적 행 상세 펼치기

전적 행 클릭 시 해당 게임 8명 전원 정보가 인라인 펼침:

| 컬럼 | 내용 |
|------|------|
| # | 최종 순위 (1위 금색, 2위 은색, 3위 동색, 4위 이하 기본색) |
| 소환사 | 소환사명 + 태그 |
| 스테이지 | last_round → Spring 변환 (예: 5 → 2-1) |
| 시너지 | 활성 시너지 아이콘 + 개수 |
| 챔피언 | 유닛 이미지 + 아이템 이미지 |
| 킬 | players_eliminated |
| 잔여골드 | gold_left |

- 증강 컬럼 없음 (Riot API 공식 스키마 미포함 — 미제공 확정)
- 내 행은 청록 하이라이트로 구분

---

## 데이터 요구사항 (Riot API)

백엔드는 아래 5개 Riot API를 조합하여 **2개의 분리된 엔드포인트**로 제공:

**① GET /api/summoners/{gameName}/{tagLine}** — 소환사 프로필 + 랭크 (단계 1~3)

| 단계 | API | 목적 |
|------|-----|------|
| 1 | account-v1 `GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}` | puuid, gameName, tagLine |
| 2 | tft-summoner-v1 `GET /tft/summoner/v1/summoners/by-puuid/{puuid}` | profileIconId, summonerLevel |
| 3 | tft-league-v1 `GET /tft/league/v1/by-puuid/{puuid}` | tier, rank, leaguePoints, wins, losses |

- 2단계·3단계는 PUUID 공통이므로 병렬 호출 가능
- 비배치(언랭크) 소환사: tier/rank=null, leaguePoints/wins/losses=0

**② GET /api/match/{puuid}/matches?start={int}&count={int}** — 전적 목록 (단계 4~5)

| 단계 | API | 목적 |
|------|-----|------|
| 4 | tft-match-v1 (IDs) `GET /tft/match/v1/matches/by-puuid/{puuid}/ids` | 매치 ID 목록 |
| 5 | tft-match-v1 (상세) `GET /tft/match/v1/matches/{matchId}` | 참가자, 유닛, 시너지 등 |

- start 기본값 0, count 기본값 20. 매치 상세 호출 간 200ms 쓰로틀 적용
- queue_id 1100(랭크) / 1090(일반)만 수집. 하이퍼롤 등 그 외 제외
- wins = placement ≤ 4 횟수, losses = placement > 4 횟수
- Riot API의 `win` 필드(1위 여부)는 사용하지 않음 — 반드시 placement로 직접 판정
- LeagueEntryDTO의 wins/losses는 Riot 서버 전체 누적값 — 최근 게임 승률 계산에 사용하지 않음
- 시너지 tone: Riot API `style`(0~4) → Spring 변환 (0=없음, 1=브론즈, 2=실버, 3=골드, 4=크로매틱)
- 챔피언 별수: Riot API `tier`(int) 그대로 사용
- LP 변동값, 증강: Riot API 미제공 — 임의 생성하지 않음

---

## 미결 사항

| 항목 | 우선순위 |
|------|---------|
| ~~API 경로 단수→복수 통일~~ `/api/summoners/**` | ~~높음~~ **완료** |
| ~~전적 최신화 엔드포인트 구현 `POST /api/summoners/{gameName}/{tagLine}/refresh`~~ | ~~중간~~ **완료** |
| 게임 유형(gameType) 변환 정책 확정 | 중간 |
| 스테이지 변환 정책 확정 (현재 level 임시값 사용 중) | 중간 |
| compositionName 자동 생성 (현재 빈 문자열) | 낮음 |
| itemImageUrls 아이템 이미지 URL 매핑 (현재 빈 배열) | 낮음 |
