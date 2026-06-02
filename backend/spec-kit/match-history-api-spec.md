**TFT 전적 검색 서비스**

API 설계 스펙문서

v1.2 (2026-06-01 팀 spec-kit 병합)

전적 검색 팀

# **1. 서비스 개요**

사용자가 gameName#tagLine을 입력하면 소환사 프로필, 랭크 정보, 최근 매치 목록 및 상세 정보를 표시하는 TFT 전적 검색 서비스입니다.

## **1.1 주요 기능**

- 소환사 프로필 조회 (아이콘, 레벨)

- 랭크 티어 / LP / 승패 표시 (일반 랭크 전용)

- 최근 매치 목록 및 상세 표시 (배치, 유닛, 특성)

- 승률 집계: placement 기준 계산 (4위 이하 순방, 5위 이상 역방)

- 덱 집계 추천 영역은 별도 팀 담당 — 본 문서 범위 외

## **1.2 기능 요구사항** *(출처: spec.md § Summoner/Match)*

- 사용자는 Riot ID 형식의 `gameName`과 `tagLine`으로 소환사를 검색할 수 있어야 한다.
- 백엔드는 Riot Account, Summoner, League, Match API를 조합해 소환사 정보를 제공한다.
- 소환사 응답에는 기본 정보, 랭크 정보, 승패, 승률, 평균 순위, TOP4 같은 화면 핵심 지표가 포함되어야 한다.
- 사용자는 최근 전적 목록을 조회할 수 있어야 한다.
- 전적 목록은 랭크/일반 게임 유형을 구분할 수 있어야 한다.
- 사용자는 전적 최신화를 요청할 수 있어야 한다.
- 매치 상세는 참가자, 순위, 스테이지, 시너지, 유닛, 아이템, 킬 수, 잔여 골드 등 프론트가 표시할 수 있는 형태로 가공해야 한다.
- **Riot API에서 제공하지 않는 LP 변동값과 증강 정보는 임의 생성하지 않는다.**

## **1.3 최소 수집 원칙**

*이 서비스는 화면 표시에 필요한 최소한의 필드만 수집합니다. 리틀레전드(CompanionDto), 승급전(MiniSeriesDTO), 스킨 정보, TURBO 전용 필드는 수집하지 않습니다.*

## **1.4 시스템 경계**

*덱 집계 추천 서버는 별도 수집 서버(또는 스케줄러)로 운영되며, 본 전적 검색 API 서버와 독립적으로 Riot API를 호출합니다. 두 서버가 공유하는 자원은 없으며, Riot API Rate Limit은 각 서버의 API Key 기준으로 독립 관리됩니다.*.

# **2. API 엔드포인트 큰틀** *(출처: plan.md)*

| **Method** | **Path** | **접근** | **목적** |
| --- | --- | --- | --- |
| `GET` | `/api/summoners/{gameName}/{tagLine}` | 공개 | 소환사 기본/랭크 정보 조회 |
| `GET` | `/api/summoners/{gameName}/{tagLine}/matches` | 공개 | 소환사 전적 목록 조회 |
| `POST` | `/api/summoners/{gameName}/{tagLine}/refresh` | 인증 또는 제한 필요 | 전적 최신화 |

> ⚠️ **경로 불일치 주의**: 현재 구현(`SummonerController.java`)은 `/api/summoner/**` (단수)를 사용한다. 팀 표준은 `/api/summoners/**` (복수)다. SecurityConfig permitAll 경로 및 프론트 `summonerApi.ts` 호출 경로와 함께 일괄 변경이 필요하다. (미결 — §9 참조)

# **3. Riot API 호출 설계**

## **3.1 호출 순서**

| **단계** | **API** | **엔드포인트** | **목적** |
| --- | --- | --- | --- |
| 1 | account-v1 | GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine} | PUUID 획득 |
| 2 | tft-summoner-v1 | GET /tft/summoner/v1/summoners/by-puuid/{encryptedPUUID} | 프로필 아이콘·레벨 |
| 3 | tft-league-v1 | GET /tft/league/v1/by-puuid/{puuid} | 티어·LP·승패 |
| 4 | tft-match-v1 | GET /tft/match/v1/matches/by-puuid/{puuid}/ids | 매치 ID 목록 |
| 5 | tft-match-v1 | GET /tft/match/v1/matches/{matchId} | 매치 상세 |

*2단계(tft-summoner-v1)와 3단계(tft-league-v1)는 PUUID를 공통 입력으로 사용하므로 병렬 호출 가능합니다.*

## **3.2 라우팅 — 지역(Region) 설정**

| **API** | **라우팅 방식** | **한국 서버 기준 호스트** |
| --- | --- | --- |
| account-v1 | 대륙 (Continental) | asia.api.riotgames.com |
| tft-summoner-v1 | 플랫폼 (Platform) | kr.api.riotgames.com |
| tft-league-v1 | 플랫폼 (Platform) | kr.api.riotgames.com |
| tft-match-v1 (매치 ID) | 대륙 (Continental) | asia.api.riotgames.com |
| tft-match-v1 (매치 상세) | 대륙 (Continental) | asia.api.riotgames.com |

## **3.3 Rate Limit 정책**

- 429 응답 수신 시: `BusinessException(ErrorCode.RIOT_API_RATE_LIMIT)` throw — 재시도 없음 (현재 구현)

- 권장 개선안: 429 응답 헤더의 Retry-After 값을 파싱하여 정확한 대기 시간 후 재시도 적용

- 로드맵 우선순위: 중간

*덱 집계 추천 팀의 수집 서버는 별도 API Key를 사용하므로, Rate Limit 버킷이 분리됩니다. 동일 Key 공유 시 상호 간섭이 발생하므로 Key를 반드시 분리해야 합니다.*

# **4. 응답 필드 정의**

## **4.1 AccountDto (account-v1)**

| **필드명** | **타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| puuid | string | O | 암호화된 PUUID. 이후 모든 API 호출의 기준 식별자 |
| gameName | string | △ | 소환사 이름 표시에 사용. 응답에 없을 수 있음 (선택 필드) |
| tagLine | string | △ | 태그라인. 응답에 없을 수 있음 (선택 필드) |

*소환사 이름 표시는 반드시 이 gameName을 사용합니다. tft-summoner-v1에는 summonerName 필드가 없습니다.*

## **4.2 SummonerDTO (tft-summoner-v1)**

아래는 우리 서비스가 실제로 매핑하여 사용하는 필드 목록입니다.

| **필드명** | **타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| puuid | string | O | 암호화된 PUUID. 정확히 78자 |
| profileIconId | int | O | 소환사 아이콘 ID. Data Dragon 이미지 URL 조합에 사용 |
| summonerLevel | long | O | 소환사 레벨 |
| revisionDate | long | O | 마지막 수정일 (epoch milliseconds) |

*Riot API tft-summoner-v1 응답에는 `id`(encryptedSummonerId), `accountId` 등 추가 필드가 포함될 수 있습니다. 우리 서비스는 화면 표시에 필요한 위 4개 필드만 사용하며, 나머지 필드는 `SummonerDto`의 `@JsonIgnoreProperties(ignoreUnknown = true)` 설정으로 자동 무시합니다.*

## **4.3 LeagueEntryDTO (tft-league-v1)**

응답은 배열이며, queueType === "RANKED_TFT" 항목만 필터링하여 사용합니다.

TURBO(RANKED_TFT_TURBO) 및 기타 큐타입은 사용하지 않습니다.

| **필드명** | **타입** | **설명** |
| --- | --- | --- |
| puuid | string | 소환사 PUUID |
| leagueId | string | 리그 ID |
| queueType | string | 큐 타입 필터 기준. RANKED_TFT만 사용 |
| tier | string | 티어 (IRON ~ CHALLENGER) |
| rank | string | 단계 (I ~ IV). 마스터 이상 해당 없음 |
| leaguePoints | int | 현재 LP |
| wins | int | 총 승리 수 (LeagueEntryDTO 자체 필드 — 매치 기반 계산값과 별개) |
| losses | int | 총 패배 수 (LeagueEntryDTO 자체 필드 — 매치 기반 계산값과 별개) |

*hotStreak, veteran, freshBlood, inactive, miniSeries(MiniSeriesDTO), ratedTier, ratedRating 필드는 사용하지 않습니다.*

## **4.4 매치 ID 목록 — 쿼리 파라미터 (tft-match-v1)**

GET /tft/match/v1/matches/by-puuid/{puuid}/ids

| **파라미터** | **타입** | **기본값** | **사용값** | **설명** |
| --- | --- | --- | --- | --- |
| start | int | 0 | 0 | 시작 인덱스. 기본값 사용 |
| count | int | 20 | 30 | 반환할 매치 ID 수. 30개로 고정 |
| queue | int | - | 미사용 | Riot API 지원 파라미터이나 사용하지 않음. 큐타입 필터링은 매치 상세 InfoDto.queue_id 기준으로 서버에서 처리 |
| startTime | long | - | 미사용 | 필터 시작 시각. 사용하지 않음 |
| endTime | long | - | 미사용 | 필터 종료 시각. 사용하지 않음 |

## **4.5 큐타입 필터링 (queue_id)**

매치 상세(InfoDto)의 queue_id를 기준으로 일반게임과 일반 랭크게임만 처리합니다.

| **queue_id** | **게임 타입** | **처리 여부** |
| --- | --- | --- |
| 1090 | 일반게임 (Normal) | O 사용 |
| 1100 | 일반 랭크게임 (Ranked) | O 사용 |
| 1110 | 튜토리얼 | X 제외 |
| 1111 | TFT 시뮬레이터 | X 제외 |
| 1130 | Hyper Roll (TURBO) | X 제외 |
| 1160 | Double Up | X 제외 |
| 기타 | 그 외 모든 큐타입 | X 제외 |

*매치 ID 목록 API에는 queue_id 필터 파라미터가 없습니다. 매치 상세를 호출한 후 queue_id를 확인하여 클라이언트(또는 서버) 측에서 필터링합니다.*

## **4.6 MatchDto → InfoDto (tft-match-v1)**

GET /tft/match/v1/matches/{matchId}

### **InfoDto 사용 필드**

| **필드명** | **타입** | **설명** |
| --- | --- | --- |
| game_datetime | long | 게임 시작 시각 (epoch ms) |
| game_length | float | 게임 길이 (초) |
| game_version | string | 패치 버전 문자열. 덱 집계 팀과 공유하는 기준 필드 |
| queue_id | int | 큐타입 필터링 기준 (3.5 참조) |
| tft_set_number | int | TFT 세트 번호 |
| tft_set_core_name | string | 세트 내부 코어명 |
| tft_game_type | string | 게임 타입 |
| participants | ParticipantDto[] | 참가자 목록 (8명) |

*Deprecated 필드: queueId, game_variation — 사용하지 않습니다.*

### **ParticipantDto 사용 필드**

| **필드명** | **타입** | **설명** |
| --- | --- | --- |
| puuid | string | 참가자 PUUID. 본인 매치 필터 기준 |
| riotIdGameName | string | 참가자 게임 이름 |
| riotIdTagline | string | 참가자 태그라인 |
| placement | int | 최종 순위 (1~8). 승패 계산 기준 |
| level | int | 소환사 레벨 |
| last_round | int | 마지막 라운드 |
| gold_left | int | 남은 골드 |
| players_eliminated | int | 처치한 플레이어 수 |
| time_eliminated | float | 탈락 시각 (초) |
| total_damage_to_players | int | 플레이어에게 준 총 피해량 |
| traits | TraitDto[] | 활성화된 특성 목록 |
| units | UnitDto[] | 배치된 유닛 목록 |

*win 필드는 사용하지 않습니다. 승패는 placement 기준으로 직접 계산합니다. companion(CompanionDto) 필드도 사용하지 않습니다.*

### **TraitDto**

| **필드명** | **타입** | **설명** |
| --- | --- | --- |
| name | string | 특성 이름 (내부 키) |
| num_units | int | 배치된 해당 특성 유닛 수 |
| style | int | 활성화 단계 스타일 (0=비활성, 1~4 등) |
| tier_current | int | 현재 활성화 단계 |
| tier_total | int | 최대 활성화 단계 |

### **UnitDto**

| **필드명** | **타입** | **설명** |
| --- | --- | --- |
| character_id | string | 유닛 내부 캐릭터 ID |
| name | string | 유닛 표시 이름 |
| tier | int | 유닛 성급 (1~3) |
| rarity | int | 유닛 등급 (코스트) |
| itemNames | string[] | 장착 아이템 이름 배열 |

*items(아이템 ID 배열)와 chosen 필드는 사용하지 않습니다. itemNames만 사용합니다.*

# **5. 승패 및 승률 계산**

## **5.1 승패 판정 기준**

| **조건** | **판정** | **설명** |
| --- | --- | --- |
| placement <= 4 | 승 (Win) | 1~4위: 순방 |
| placement > 4 | 패 (Loss) | 5~8위: 역방 |

*Riot API의 win 필드(boolean)는 1위 여부를 나타내므로 사용하지 않습니다. 반드시 placement 값으로 직접 판정합니다.*

## **5.2 승률 계산**

winRate = wins / (wins + losses) * 100

- wins: 해당 매치 목록에서 placement <= 4인 게임 수

- losses: 해당 매치 목록에서 placement > 4인 게임 수

- 표시: 소수점 1자리 반올림 (예: 66.7%)

- 전체 게임 수가 0인 경우: 승률 표시 없음

*LeagueEntryDTO의 wins/losses 필드는 Riot 서버 전체 누적값이므로, 최근 30게임 승률 계산에는 사용하지 않습니다. 매치 상세의 placement 기반 계산값을 사용합니다.*

# **6. 화면 구성 및 데이터 매핑**

## **6.1 소환사 프로필 영역**

| **UI 요소** | **데이터 출처** | **필드** |
| --- | --- | --- |
| 소환사 이름 | account-v1 → AccountDto | gameName |
| 태그라인 | account-v1 → AccountDto | tagLine |
| 프로필 아이콘 | tft-summoner-v1 → SummonerDTO | profileIconId → Data Dragon URL 조합 |
| 소환사 레벨 | tft-summoner-v1 → SummonerDTO | summonerLevel |

## **6.2 랭크 정보 영역**

| **UI 요소** | **데이터 출처** | **필드 / 조건** |
| --- | --- | --- |
| 티어·단계 | tft-league-v1 → LeagueEntryDTO | tier + rank (queueType === RANKED_TFT 필터) |
| LP | tft-league-v1 → LeagueEntryDTO | leaguePoints |
| 누적 승/패 | tft-league-v1 → LeagueEntryDTO | wins / losses (Riot 서버 전체 누적) |
| 최근 승률 | 매치 계산값 | placement 기반 wins / (wins + losses) * 100 |

## **6.3 매치 목록 영역**

| **UI 요소** | **데이터 출처** | **필드** |
| --- | --- | --- |
| 게임 시각 | InfoDto | game_datetime |
| 게임 시간 | InfoDto | game_length |
| 패치 버전 | InfoDto | game_version (파싱 후 표시) |
| 큐타입 | InfoDto | queue_id → 일반/랭크 레이블 |
| 순위 | ParticipantDto | placement |
| 레벨 | ParticipantDto | level |
| 남은 골드 | ParticipantDto | gold_left |
| 처치 플레이어 수 | ParticipantDto | players_eliminated |
| 특성 목록 | ParticipantDto → TraitDto[] | name, tier_current, style |
| 유닛 목록 | ParticipantDto → UnitDto[] | character_id, tier, rarity, itemNames |

# **7. 덱 집계 추천 영역 — 인터페이스 정의**

*이 섹션은 덱 집계 추천 팀과의 충돌을 방지하기 위한 경계 정의입니다. 집계 로직 자체는 해당 팀 스펙문서를 따릅니다.*

## **7.1 공유 데이터 필드**

| **필드** | **출처** | **전적 검색 사용 목적** | **집계 팀 사용 목적** |
| --- | --- | --- | --- |
| game_version | InfoDto | 패치 표시 | 패치 기준 집계 |
| queue_id | InfoDto | 일반/랭크 필터링 | 집계 대상 큐타입 필터 |
| puuid | AccountDto / ParticipantDto | 본인 매치 필터 | 소환사 식별 |
| placement | ParticipantDto | 순위 표시 및 승패 계산 | 덱 성과 집계 기준 |
| traits, units | ParticipantDto | 매치 상세 표시 | 덱 구성 집계 |

## **7.2 독립 운영 원칙**

- 전적 검색 API 서버와 덱 집계 수집 서버는 동일한 Riot API 엔드포인트를 각자의 API Key로 독립 호출합니다.

- 두 서버 간 직접 통신 또는 데이터 공유 없음. 집계 결과는 덱 집계 팀이 자체 DB에 저장합니다.

- 전적 검색 서버에서 집계 결과를 조회해야 할 경우, 별도 내부 API 엔드포인트를 협의하여 정의합니다. (현재 미정)

## **7.3 패치 버전 파싱 규칙**

"Version 14.12.639.9834 (Sep 10 2024/11:11:11) [PUBLIC] <Releases/14.12>"

- 주요 버전(예: 14.12) 추출: 정규식 `(\d+\.\d+[a-zA-Z]?)` 으로 첫 번째 매칭값 사용

- 구현 위치: `MetaDeckServiceImpl.normalizePatchVersion()` — 덱 집계 팀 서버에서 이미 운영 중

- 전적 검색 팀도 동일한 정규식을 적용해야 패치 기준이 일치합니다. (구현 완료)

# **8. 에러 처리**

## **8.1 Riot API 에러 코드**

| **HTTP 상태** | **의미** | **처리 방침** |
| --- | --- | --- |
| 400 | 잘못된 요청 | 클라이언트에 입력 오류 안내 |
| 401 | API Key 없음 또는 만료 | 서버 로그 기록, 관리자 알림 |
| 403 | API Key 권한 없음 | 서버 로그 기록 |
| 404 | 소환사 또는 매치 없음 | 사용자에게 검색 결과 없음 표시 |
| 429 | Rate Limit 초과 | `BusinessException(ErrorCode.RIOT_API_RATE_LIMIT)` throw, 재시도 없음. 개선안: Retry-After 헤더 파싱 후 재시도 |
| 500 / 503 | Riot 서버 오류 | 일시적 오류 안내, 재시도 유도 |

## **8.2 부분 실패 처리**

- 2단계(summoner)·3단계(league) 병렬 호출 중 하나가 실패해도 나머지 데이터는 표시합니다.

- 매치 상세 호출 실패 시 해당 매치만 오류 표시하고 나머지 목록은 정상 표시합니다.

- league 정보 없음(배열 빈값): 비배치 상태로 처리하여 배치 미완료를 표시합니다.

- queue_id 필터로 제외된 매치는 목록에서 제외하며, 오류로 처리하지 않습니다.

# **9. 미결 사항 및 로드맵**

| **항목** | **우선순위** | **비고** |
| --- | --- | --- |
| **API 경로 단수→복수 통일** | **높음** | `/api/summoner/**` → `/api/summoners/**` (SummonerController, SecurityConfig, summonerApi.ts 동시 변경 필요) |
| Rate Limit — Retry-After 헤더 파싱 후 재시도 적용 | 중간 | 현재: 예외 throw만, 재시도 없음 |
| 전적 최신화 엔드포인트 구현 | 중간 | `POST /api/summoners/{gameName}/{tagLine}/refresh` 미구현 |
| 게임 유형(gameType) 변환 정책 확정 | 중간 | queue_id → RANKED/NORMAL 레이블 매핑 기준 명문화 필요 |
| 스테이지 변환 정책 확정 | 중간 | 현재 임시로 level 값 사용 중. 실제 라운드 번호 표기 방식 협의 필요 |
| ~~game_version 파싱 규칙 공유 여부 협의~~ | ~~높음~~ | 완료 — `normalizePatchVersion()` 정규식 `(\d+\.\d+[a-zA-Z]?)` 양 팀 공유 기준 확정 |
| 집계 결과 조회용 내부 API 엔드포인트 정의 | 미정 | 집계 팀과 협의 후 문서화 |
| Data Dragon 버전 동기화 방식 | 낮음 | profileIconId → 아이콘 이미지 URL 조합 기준 |

---

# **10. 검증 체크리스트** *(출처: checklist.md § Summoner/Match)*

- [ ] Riot ID 기반 소환사 검색이 동작한다.
- [ ] 소환사 기본 정보와 랭크 정보가 응답에 포함된다.
- [ ] 전적 목록이 최신순으로 반환된다.
- [ ] 랭크/일반 게임 유형이 구분된다.
- [ ] 매치 상세에 참가자, 유닛, 아이템, 시너지 정보가 포함된다.
- [ ] Riot API 미제공 데이터(LP 변동, 증강)는 임의 생성하지 않는다.
- [ ] Riot API 실패와 rate limit이 처리된다.
- [ ] 프론트 `axiosInstance` baseURL과 백엔드 경로가 중복되지 않는다.
- [ ] 프론트 API 호출 경로와 백엔드 path, query parameter가 일치한다.