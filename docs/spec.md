# spec.md - 비즈니스 요구사항 정의

> 이 문서는 TFT-gogo가 사용자에게 제공해야 하는 기능과 데이터 정책을 정리한다.
> 구현 방식보다 "무엇을 제공해야 하는가"에 집중한다.

## 문서 기준

- 기준 브랜치: `develop`
- 기준 시점: 2026-06-01
- 최신 반영 상태:
  - 메타 덱 중복 병합 및 관리자 덱 큐레이션 기능은 현재 `develop` 기준에 포함되어 있다.
  - 인증 페이지 UI 작업과 Guide/PatchNotes 일부 리팩터링 작업은 최근 `develop`에서 revert되어 현재 구현 기준에서 제외한다.
  - Guide/PatchNotes 관리자 연동 대비 백엔드 계약 정리는 이슈 #110으로 별도 추적한다.

---

## 서비스 개요

TFT-gogo는 TFT 전적 검색 및 메타 가이드 서비스다.

사용자는 소환사 전적, 현재 메타 덱, 덱 상세 정보, 게임 가이드, 패치노트를 확인할 수 있다. 서비스의 핵심 데이터는 Riot API, Community Dragon, 내부 집계 데이터, 그리고 관리자 큐레이션 데이터를 조합해 제공한다.

---

## 페이지별 기능 정의

### 1. Dashboard - 메인/전적 검색

- 소환사명과 태그라인을 입력해 전적을 검색할 수 있다.
- 최근 검색어 또는 빠른 접근 항목을 통해 이전 검색 대상에 다시 접근할 수 있다.
- 검색 결과에는 소환사 기본 정보와 랭크 정보가 표시된다.
- 소환사 검색 전에도 현재 패치 기준 메타 덱 요약을 노출할 수 있다.
- 메타 덱 요약은 등급, 덱 이름, 주요 챔피언, 승률, TOP4, 평균 등수, 픽률 같은 핵심 지표를 제공한다.

### 2. Decks - 메타 덱 목록

- 현재 패치 기준 추천 메타 덱 목록을 확인할 수 있다.
- 랭크 구간 필터를 제공한다.
  - `MASTER_PLUS`
  - `DIAMOND_PLUS`
  - `EMERALD_PLUS`
- 덱은 등급 기준으로 그룹화해 표시한다.
  - `S`, `A+`, `A`, `B`, `C`, `D`
- 덱 이름은 자동 집계 이름을 기본으로 사용하되, 관리자 큐레이션 이름이 있으면 큐레이션 이름을 우선 표시한다.
- 관리자가 숨김 처리한 덱은 공개 목록에 노출하지 않는다.
- 관리자가 정렬 우선순위를 지정한 덱은 기본 통계 정렬보다 우선해 노출할 수 있다.
- 덱 카드에는 주요 챔피언, 시너지, 승률, TOP4, 평균 등수, 픽률, 표본 수를 표시한다.

### 3. DeckDetail - 메타 덱 상세

- 덱 상세에서는 추천 배치, 레벨별 유닛 구성, 시너지 구성, 핵심 아이템, 통계 지표를 확인할 수 있다.
- 추천 배치는 레벨별로 확인할 수 있다.
  - Lv.5 ~ Lv.9
- 레벨 변경 시 현재 노출 유닛 기준으로 시너지 구성이 함께 갱신되어야 한다.
- 유닛 목록은 최종 운영 레벨 기준으로 필터링한다.
  - 5코스트 캐리 덱: Lv.9 기준
  - 4코스트 캐리 덱: Lv.8 기준
  - 3코스트 리롤 덱: Lv.7 기준
  - 1~2코스트 리롤 덱: Lv.8 기준
- 증강체 추천은 Riot API에서 안정적으로 제공되지 않으므로 자동 집계 핵심 범위에 포함하지 않는다.
- 증강체 추천이 필요할 경우 추후 관리자 큐레이션 데이터로 제공한다.

### 4. AiRecommend - AI 덱 추천

- 사용자의 현재 보유 유닛과 아이템 상태를 기반으로 추천 덱을 제안한다.
- 추천 결과에는 추천 덱 이름, 핵심 유닛, 아이템 배치, 운영 방향을 포함할 수 있다.
- AI 추천은 메타 덱 데이터와 별개로 동작하되, 메타 덱 데이터를 참고 정보로 활용할 수 있다.

### 5. Guide - 게임 가이드

담당 범위: 게임가이드 공개 화면 및 추후 관리자 연동 대비 데이터 계약.

- `/guide`에서 TFT 기본 가이드를 확인할 수 있다.
- 가이드는 다음 탭으로 구성한다.
  - 시너지
  - 아이템
  - 증강체
  - 챔피언
- 각 탭은 검색, 필터, 정렬, 페이지네이션을 제공할 수 있다.
- 공개 화면은 사용자에게 노출 가능한 가이드 데이터만 표시한다.
- Community Dragon은 챔피언, 아이템, 특성, 증강체의 이름/아이콘/이미지 참조용으로 사용한다.
- 프론트는 백엔드 API가 준비되면 `/guide`, `/guide/{tab}` 응답을 우선 사용하고, 응답이 없거나 유효하지 않으면 fallback 데이터를 사용할 수 있다.

#### 추후 관리자 연동 시 필요한 관리 기준

- 가이드 데이터는 패치 버전 기준으로 관리되어야 한다.
- 각 항목은 노출 여부와 정렬 순서를 가질 수 있어야 한다.
- 관리자는 항목의 이름, 요약, 이미지 참조, 상세 JSON 데이터를 수정할 수 있어야 한다.
- 관리자 페이지 자체 구현은 이 범위에 포함하지 않는다.
- 백엔드 API/DTO 계약 정리는 이슈 #110에서 별도로 처리한다.

#### 백엔드 API 계약 초안

공개 API는 사용자 화면에서 바로 사용할 수 있는 데이터만 반환한다.

| Method | Path | 목적 |
| --- | --- | --- |
| `GET` | `/api/guide` | 공개 가이드 카탈로그 전체 조회 |
| `GET` | `/api/guide/{tab}` | 탭별 공개 가이드 목록 조회 |

- `{tab}` 값은 `traits`, `items`, `augments`, `champions` 중 하나를 사용한다.
- 공개 API는 `published = true`인 데이터만 반환한다.
- `patchVersion`이 없으면 최신 공개 패치 버전을 기준으로 조회한다.
- 탭별 조회는 `query`, `page`, `pageSize`, `sortKey`, `sortDir`를 지원할 수 있다.
- 챔피언 탭은 `cost` 필터를 추가로 지원할 수 있다.
- 응답은 프론트의 현재 normalizer가 이해할 수 있는 구조를 우선 유지한다.

관리자 API는 관리자 페이지가 완성된 뒤 연결할 수 있도록 계약만 먼저 정의한다.

| Method | Path | 목적 |
| --- | --- | --- |
| `GET` | `/api/admin/guides` | 가이드 항목 전체 조회 |
| `POST` | `/api/admin/guides` | 가이드 항목 생성 |
| `PATCH` | `/api/admin/guides/{guideId}` | 가이드 항목 수정 |
| `DELETE` | `/api/admin/guides/{guideId}` | 가이드 항목 삭제 또는 비활성화 |

- 관리자 API는 `X-Admin-Token` 기반 인증을 우선 사용한다.
- 추후 인증 체계가 정리되면 `ROLE_ADMIN` 권한으로 전환한다.
- 삭제는 실제 삭제와 숨김 처리 중 운영 정책에 맞춰 결정한다. 초기에는 데이터 복구 가능성을 고려해 숨김 처리를 우선 검토한다.

#### Guide DTO 기준

초기 백엔드 계약은 범용 가이드 엔트리 형태를 우선 사용한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 가이드 항목 ID |
| `guideType` | enum | `TRAIT`, `ITEM`, `AUGMENT`, `CHAMPION` |
| `targetKey` | string | CDragon 또는 내부 기준 식별자 |
| `name` | string | 화면 표시 이름 |
| `summary` | string | 요약 설명 |
| `imageUrl` | string | 대표 이미지 URL 또는 CDragon 참조 URL |
| `patchVersion` | string | 기준 패치 버전 |
| `dataJson` | object/string | 탭별 상세 렌더링에 필요한 확장 데이터 |
| `published` | boolean | 공개 노출 여부 |
| `sortOrder` | number | 관리자 지정 정렬 순서 |
| `createdAt` | datetime | 생성 시각 |
| `updatedAt` | datetime | 수정 시각 |

- `dataJson`은 초기에는 탭별 화면 요구사항을 빠르게 수용하기 위한 확장 필드로 둔다.
- 검색/정렬/필터에 자주 쓰이는 값은 추후 별도 컬럼으로 승격할 수 있다.
- 프론트가 직접 계산하기 어려운 표시 순서와 노출 여부는 백엔드/관리자 데이터 기준을 따른다.

### 6. PatchNotes - 패치노트

담당 범위: 패치노트 공개 화면 및 추후 관리자 연동 대비 데이터 계약.

- `/patch-notes`에서 최신 패치노트와 이전 패치 히스토리를 확인할 수 있다.
- 패치노트는 버전, 제목, 적용일, 설명, 핵심 요약, 대표 이미지를 제공한다.
- 변경사항은 카테고리별로 확인할 수 있다.
  - 챔피언
  - 시너지
  - 아이템
  - 증강체
  - 시스템
- 변경사항은 변경 타입별로 필터링할 수 있다.
  - 상향
  - 하향
  - 조정
  - 신규
- 영향도가 높은 변경사항만 모아볼 수 있어야 한다.
- 검색어와 페이지네이션을 통해 변경사항을 탐색할 수 있다.
- 프론트는 백엔드 API가 준비되면 `/patch-notes`, `/patch-notes/{version}/changes` 응답을 우선 사용하고, 응답이 없거나 유효하지 않으면 fallback 데이터를 사용할 수 있다.

#### 추후 관리자 연동 시 필요한 관리 기준

- 패치노트는 버전 단위로 관리되어야 한다.
- 각 패치노트는 현재 버전 여부, 노출 여부, 정렬 기준을 가질 수 있어야 한다.
- 변경사항은 카테고리, 변경 타입, 영향도, 대상, 변경 전/후 값, 태그를 구조화해 관리해야 한다.
- 관리자 페이지 자체 구현은 이 범위에 포함하지 않는다.
- 백엔드 API/DTO 계약 정리는 이슈 #110에서 별도로 처리한다.

#### 백엔드 API 계약 초안

공개 API는 패치노트 목록과 선택한 패치 버전의 변경사항을 분리해서 제공한다.

| Method | Path | 목적 |
| --- | --- | --- |
| `GET` | `/api/patch-notes` | 공개 패치노트 목록 조회 |
| `GET` | `/api/patch-notes/{version}/changes` | 특정 버전 변경사항 페이지 조회 |

- 공개 API는 `published = true`인 패치노트와 변경사항만 반환한다.
- 패치노트 목록은 현재 패치가 먼저 오고, 이후 적용일 또는 정렬 순서 기준으로 정렬한다.
- 변경사항 조회는 `category`, `type`, `impact`, `query`, `page`, `pageSize`를 지원할 수 있다.
- `category` 값은 `CHAMPION`, `TRAIT`, `ITEM`, `AUGMENT`, `SYSTEM`을 사용한다.
- `type` 값은 `BUFF`, `NERF`, `ADJUST`, `NEW`를 사용한다.
- `impact` 값은 `HIGH`, `MEDIUM`, `LOW`를 사용한다.
- 변경사항 응답에는 페이지 정보와 통계 요약을 함께 포함한다.

관리자 API는 패치노트 본문과 변경사항을 관리할 수 있도록 분리한다.

| Method | Path | 목적 |
| --- | --- | --- |
| `GET` | `/api/admin/patch-notes` | 패치노트 전체 조회 |
| `POST` | `/api/admin/patch-notes` | 패치노트 생성 |
| `PATCH` | `/api/admin/patch-notes/{patchNoteId}` | 패치노트 수정 |
| `DELETE` | `/api/admin/patch-notes/{patchNoteId}` | 패치노트 삭제 또는 비활성화 |
| `GET` | `/api/admin/patch-notes/{patchNoteId}/changes` | 관리자용 변경사항 조회 |
| `POST` | `/api/admin/patch-notes/{patchNoteId}/changes` | 변경사항 생성 |
| `PATCH` | `/api/admin/patch-note-changes/{changeId}` | 변경사항 수정 |
| `DELETE` | `/api/admin/patch-note-changes/{changeId}` | 변경사항 삭제 또는 비활성화 |

- 관리자 API는 `X-Admin-Token` 기반 인증을 우선 사용한다.
- 현재 버전은 하나만 유지하는 정책을 우선 검토한다.
- 삭제 정책은 운영 편의상 숨김 처리 또는 soft delete를 우선 검토한다.

#### PatchNotes DTO 기준

패치노트 요약 DTO는 목록과 히어로 영역에서 사용할 수 있어야 한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 패치노트 ID |
| `version` | string | 패치 버전 |
| `title` | string | 제목 |
| `summary` | string | 짧은 요약 |
| `description` | string | 상세 설명 |
| `focus` | string | 이번 패치 핵심 문구 |
| `representativeImageUrl` | string | 대표 이미지 |
| `publishedAt` | date/datetime | 적용일 또는 게시일 |
| `isCurrent` | boolean | 현재 패치 여부 |
| `published` | boolean | 공개 노출 여부 |
| `sortOrder` | number | 관리자 지정 정렬 순서 |
| `highlights` | string[] | 핵심 요약 목록 |

패치 변경사항 DTO는 필터링 가능한 구조를 유지해야 한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 변경사항 ID |
| `category` | enum | `CHAMPION`, `TRAIT`, `ITEM`, `AUGMENT`, `SYSTEM` |
| `type` | enum | `BUFF`, `NERF`, `ADJUST`, `NEW` |
| `impact` | enum | `HIGH`, `MEDIUM`, `LOW` |
| `targetKey` | string | CDragon 또는 내부 기준 식별자 |
| `targetName` | string | 변경 대상 표시 이름 |
| `summary` | string | 변경 요약 |
| `beforeValue` | string | 변경 전 값 |
| `afterValue` | string | 변경 후 값 |
| `imageUrl` | string | 대상 이미지 URL 또는 참조 URL |
| `tags` | string[] | 검색/분류용 태그 |
| `sortOrder` | number | 관리자 지정 정렬 순서 |

- 변경사항 페이지 응답은 `items`, `page`, `pageSize`, `totalItems`, `totalPages`, `stats`를 포함한다.
- `stats`에는 전체 변경 수, 카테고리별 수, 변경 타입별 수, 상향/하향 수, 높은 영향도 변경 수를 포함한다.
- 프론트 표기 언어는 프론트 normalizer에서 처리할 수 있으므로 백엔드 enum은 영문 고정값을 우선 사용한다.

### 7. Admin - 관리자 큐레이션

현재 `develop` 기준으로 관리자 기능은 메타 덱 큐레이션에 한정한다.

- `/admin`에서 관리자 덱 큐레이션을 수행할 수 있다.
- 관리자 API는 `X-Admin-Token` 헤더 기반 인증을 사용한다.
- 관리자는 자동 집계 덱에 대해 다음 값을 수정할 수 있다.
  - 표시 이름
  - 숨김 여부
  - 정렬 우선순위
  - 관리자 메모
- 큐레이션 데이터는 자동 집계 데이터를 삭제하지 않고 별도 레이어로 보관한다.
- 추후 인증 체계가 정리되면 `ROLE_ADMIN` 기반 권한으로 전환할 수 있다.
- Guide/PatchNotes 관리자 기능은 아직 구현 범위가 아니며, 추후 별도 관리자 페이지와 백엔드 API가 준비될 때 연결한다.

---

## 데이터 관리 정책

### 메타 덱 데이터

- 메타 덱은 Riot 매치 데이터를 기반으로 자동 집계한다.
- 자동 집계만으로는 덱 이름, 중복 덱, 노출 순서의 품질을 완전히 보장하기 어렵다.
- 따라서 공개 목록에는 관리자 큐레이션 레이어를 함께 적용한다.
- 대량 매치 데이터 정제 비용이 큰 기능은 무리하게 프론트에서 해결하지 않는다.

### Guide/PatchNotes 데이터

- 게임가이드와 패치노트는 B2C 공개 콘텐츠이므로 정확성과 관리 가능성이 중요하다.
- 대량 자동 수집보다 관리자 큐레이션 가능한 구조를 우선 고려한다.
- 관리자가 나중에 수정할 가능성이 있는 값은 프론트에 고정하지 않는다.
- 공개 API는 노출 가능한 데이터만 반환해야 한다.
- 관리자 API는 전체 데이터 조회, 생성, 수정, 삭제, 노출 상태 변경을 지원할 수 있어야 한다.

### Community Dragon 사용 기준

- Community Dragon은 이미지와 정적 게임 데이터 참조용으로 사용한다.
- 컴포넌트 내부에 CDN URL을 직접 하드코딩하지 않는다.
- 프론트에서는 `communityDragonAssets.ts` 계열 헬퍼를 통해 이미지 URL을 생성한다.
- DB에는 가능한 한 이미지 URL 원문보다 `championKey`, `itemKey`, `traitKey`, `augmentKey` 같은 식별자 중심 저장을 우선 검토한다.

---

## 공통 요구사항

- 모든 API 응답은 백엔드 공통 응답 포맷을 따른다.
- 프론트는 API 호출을 `axiosInstance`와 `api/` 계층을 통해 수행한다.
- 서버 상태는 TanStack Query로 관리한다.
- 공개 화면은 로딩, 에러, 빈 상태를 명확히 처리해야 한다.
- 관리자 기능은 공개 사용자 흐름과 분리되어야 한다.
