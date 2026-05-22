# ERD 추가 테이블 가이드

> 기존 팀 ERD (14개 테이블) 기반, 누락된 테이블 9개를 추가로 제안합니다.
> DDL 파일: `erd-additional-tables.sql`

---

## 추가 이유 요약

| 테이블 | 근거 |
|--------|------|
| `refresh_tokens` | JWT 구조상 필수 – 로그아웃/강제 폐기 불가 |
| `patch_notes` | 패치노트 페이지(`/patch-notes`) 데이터 |
| `guides` | 가이드 페이지(`/guides`) 데이터 |
| `meta_decks` | 덱 모음 · 메타통계 핵심 데이터 |
| `deck_traits` | meta_decks 세부 – 특성(시너지) 구성 |
| `deck_units` | meta_decks 세부 – 챔피언 구성 |
| `hero_augments` | 덱 페이지 영웅 증강 추천/비추천 섹션 |
| `artifact_stats` | 덱 페이지 유물 통계 섹션 |
| `ai_recommendations` | AI 추천 페이지 결과 캐싱 (OpenAI 비용 절감) |

---

## 테이블별 상세 설명

### 1. `refresh_tokens`
JWT Access Token(30분)과 쌍을 이루는 Refresh Token(7일) 저장소.

- `revoked_at` 컬럼으로 로그아웃 또는 재발급 시 **즉시 무효화** 가능
- token 값은 **해시 저장 권장** (평문 저장 시 DB 탈취 위험)
- `user_id` → `users(id)` ON DELETE CASCADE: 회원 탈퇴 시 자동 삭제

---

### 2. `patch_notes`
Riot 공식 TFT 패치노트 보관.

- `version` UNIQUE: 같은 패치버전 중복 방지
- `summary`: 목록 카드에 표시할 한 줄 요약
- 운영 방식: Riot 공식 사이트 크롤링 or 수동 등록

---

### 3. `guides`
운영자·유저가 작성하는 TFT 공략 가이드.

- `category` ENUM: BEGINNER / INTERMEDIATE / ADVANCED / MECHANICS
- `is_pinned`: 관리자가 상단 고정 가능
- `deleted_at`: Soft Delete (posts 테이블과 동일한 패턴)

---

### 4~6. `meta_decks` + `deck_traits` + `deck_units`
덱 모음 페이지의 핵심 3테이블 (1:N:N 구조).

```
meta_decks (1)
  ├── deck_traits (N)   ← 특성 구성
  └── deck_units  (N)   ← 챔피언 + 추천 아이템
```

- `patch_version`: 패치마다 메타가 바뀌므로 버전별 분리
- `tier`: S/A/B/C 티어 표시용
- `deck_units.recommended_items`: CDragon item ID 배열 (JSON)
- `deck_units.is_carry`: 캐리 챔피언 강조 표시용

---

### 7. `hero_augments`
덱 페이지 영웅 증강 카드 섹션 (좌측 → 추천, 우측 → 비추천).

- `is_recommended`: 1=추천, 0=비추천
- `sort_order`: 낮을수록 왼쪽에 배치 (가장 효율적인 증강이 첫 번째)
- `character_id`: 증강을 부여할 챔피언 CDragon ID

---

### 8. `artifact_stats`
덱 페이지 하단 유물(아이템) 통계 섹션.

- `deck_id = NULL`: 전체 게임 기준 통계
- `deck_id = N`: 특정 덱 사용 시 통계
- `placement_delta`: 해당 유물 장착 시 **평균 등수 대비 변화값** (음수 = 더 좋은 등수)
- UNIQUE: `(patch_version, deck_id, item_id)` 중복 집계 방지

---

### 9. `ai_recommendations`
AI 추천 페이지 응답 캐시.

- `request_hash`: 소환사 PUUID + 최근 매치 데이터를 SHA-256 해싱한 값
- 동일 요청이면 DB에서 캐시 반환 → **OpenAI API 호출 절감**
- `expires_at`: TTL 기반 자동 만료 (배치 작업으로 정기 삭제 권장)
- `user_id = NULL`: 비로그인 유저도 PUUID 기반으로 캐싱 가능

---

## ERD Cloud 적용 방법

1. [erd-additional-tables.sql](./erd-additional-tables.sql) 전체 복사
2. ERD Cloud 열기 → 우측 상단 **SQL 가져오기** 클릭
3. 붙여넣기 후 **적용**
4. 자동 생성된 테이블을 기존 ERD 옆에 배치하고 관계선 연결

---

## 기존 ERD 보완 사항 (수정 제안)

기존 14개 테이블에서 발견한 보완 포인트:

| 위치 | 현재 | 제안 |
|------|------|------|
| `chat_rooms` | `type` 컬럼 없음 | `ENUM('PUBLIC','PRIVATE','PARTY')` 추가 |
| `match_participants` | augments 컬럼 없음 | Set 17 기준 정상 (API에 augments[] 없음) ✅ |
| `posts` | like_count 비정규화 | likes 테이블 COUNT 또는 Redis 카운터로 관리 고려 |
| `summoner_accounts` | rank 컬럼 있음 | LP 갱신 시 `last_updated_at` 추가 권장 |
