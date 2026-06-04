# spec.md - 백엔드 기능 요구사항 정의

> 백엔드가 사용자와 프론트에 무엇을 제공해야 하는가를 정의한다.
> 특정 담당자 기능의 상세 DTO보다 팀 전체 기능 요구사항과 도메인 책임을 우선한다.

---

## 문서 기준

- 기준 브랜치: `develop`
- 기준 시점: 2026-06-01
- 대상 모듈: `backend/`
- 목적: 팀 전체 백엔드 기능 요구사항 정리

---

## 서비스 내 백엔드 역할

백엔드는 TFT-gogo의 API 서버다. 프론트가 직접 처리하기 어려운 Riot API 연동, 대량 데이터 집계, 도메인 데이터 저장, 관리자 큐레이션, 인증/권한, AI 서버 중계를 담당한다.

백엔드는 다음 원칙을 만족해야 한다.

- 프론트가 화면에 바로 사용할 수 있는 응답 DTO를 제공한다.
- 외부 API 응답을 그대로 노출하지 않는다.
- 공개 API와 관리자 API를 명확히 분리한다.
- 대량 데이터 필터링과 정제는 백엔드 또는 DB 레벨에서 처리한다.
- 관리자 큐레이션이 필요한 데이터는 공개 데이터와 관리 데이터를 구분한다.
- 모든 응답은 공통 응답 포맷을 사용한다.

---

## 공통 기능 요구사항

### 공통 응답

- 모든 API는 `ApiResponse<T>` 형식으로 응답한다.
- 성공 응답은 `success`, `message`, `data`를 포함할 수 있다.
- 실패 응답은 공통 예외 처리기를 통해 일관된 메시지와 HTTP 상태를 반환한다.
- 목록/페이지 API는 프론트가 동일하게 처리할 수 있는 페이지 메타 정보를 제공한다.

### 공통 오류 처리

- 존재하지 않는 리소스는 404 계열 오류로 처리한다.
- 인증이 필요한 요청에 인증 정보가 없으면 401 계열 오류로 처리한다.
- 권한이 부족하면 403 계열 오류로 처리한다.
- 검증 실패는 400 계열 오류로 처리한다.
- 외부 API 장애는 프론트가 재시도 또는 오류 안내를 할 수 있도록 명확히 구분한다.

### 인증과 권한

- 회원/인증 API는 공개로 접근할 수 있어야 한다.
- 사용자 개인 정보나 쓰기 작업은 인증 기반 보호를 적용한다.
- 관리자 API는 `/api/admin/**` 아래에 두고 관리자 권한을 요구한다.
- 초기 관리자 기능은 `X-Admin-Token`을 사용하고, 추후 `ROLE_ADMIN` 권한으로 전환할 수 있어야 한다.

### 외부 데이터

- Riot API는 소환사, 랭크, 매치, 메타 덱 집계의 원천 데이터로 사용한다.
- Community Dragon은 챔피언, 아이템, 시너지, 증강체 정적 정보와 이미지 식별자 기준으로 사용한다.
- 외부 API rate limit과 실패 상황을 고려해야 한다.
- 외부 API 키와 민감한 설정값은 응답이나 로그에 노출하지 않는다.

---

## 도메인별 요구사항

도메인별 구현 PR은 기능 단위로 분리한다.

- 게임가이드와 패치노트는 서로 다른 사용자 화면, 도메인 데이터, 관리자 흐름을 가지므로 구현 이슈와 PR을 분리한다.
- 두 기능은 `ApiResponse`, 페이지 응답 규칙, 관리자 인증 방식 같은 공통 기반만 공유한다.
- Entity, Repository, Service, Controller, DTO, 테스트는 게임가이드와 패치노트 기능별로 분리한다.

### 1. Member/Auth

- 사용자는 회원가입과 로그인을 할 수 있어야 한다.
- 로그인 성공 시 프론트가 인증 상태를 유지할 수 있는 토큰 또는 세션 정보를 제공해야 한다.
- 비밀번호 같은 민감 정보는 안전하게 저장해야 한다.
- 인증 실패, 중복 이메일, 존재하지 않는 사용자 같은 오류를 명확히 구분한다.
- 추후 관리자 권한 전환을 고려해 사용자 권한 모델을 확장 가능하게 둔다.

### 2. Summoner/Match

- 사용자는 Riot ID 형식의 `gameName`과 `tagLine`으로 소환사를 검색할 수 있어야 한다.
- 백엔드는 Riot Account, Summoner, League, Match API를 조합해 소환사 정보를 제공한다.
- 소환사 응답에는 기본 정보, 랭크 정보, 승패, 승률, 평균 순위, TOP4 같은 화면 핵심 지표가 포함되어야 한다.
- 사용자는 최근 전적 목록을 조회할 수 있어야 한다.
- 전적 목록은 랭크/일반 게임 유형을 구분할 수 있어야 한다.
- 사용자는 전적 최신화를 요청할 수 있어야 한다.
- 매치 상세는 참가자, 순위, 스테이지, 시너지, 유닛, 아이템, 킬 수, 잔여 골드 등 프론트가 표시할 수 있는 형태로 가공해야 한다.
- Riot API에서 제공하지 않는 LP 변동값과 증강 정보는 임의 생성하지 않는다.

### 3. Deck

- 사용자는 현재 패치 기준 메타 덱 목록을 조회할 수 있어야 한다.
- 메타 덱은 Riot Match 데이터를 기반으로 집계한다.
- 메타 덱은 랭크 구간별로 조회할 수 있어야 한다.
- 덱 목록에는 덱 이름, 티어, 주요 챔피언, 시너지, 승률, TOP4, 평균 등수, 픽률, 표본 수가 포함되어야 한다.
- 사용자는 덱 상세에서 추천 배치, 레벨별 유닛 구성, 시너지 구성, 추천 아이템, 통계 정보를 확인할 수 있어야 한다.
- 상점 구매 불가 유닛이나 임시 소환 유닛은 공개 덱 구성에서 제외해야 한다.
- 관리자는 자동 집계 덱의 표시 이름, 숨김 여부, 정렬 우선순위, 메모를 수정할 수 있어야 한다.
- 공개 덱 API는 관리자 큐레이션을 반영해 숨김 덱을 노출하지 않아야 한다.

### 4. Guide

- 사용자는 게임가이드 데이터를 조회할 수 있어야 한다.
- 게임가이드는 패치노트와 별도 기능으로 구현하며, 게임가이드 구현 PR에는 패치노트 Entity, Controller, DTO 변경을 포함하지 않는다.
- 가이드는 시너지, 아이템, 증강체, 챔피언 같은 탭 또는 유형으로 구분될 수 있어야 한다.
- 가이드 데이터는 패치 버전, 공개 여부, 정렬 순서를 가질 수 있어야 한다.
- 공개 API는 사용자에게 노출 가능한 데이터만 반환해야 한다.
- 관리자가 나중에 데이터를 수정하거나 추가할 수 있는 구조여야 한다.
- 게임가이드는 Riot 매치 데이터 수만 건을 실시간으로 분석해 자동 생성하기보다, 관리자 큐레이션 기반 콘텐츠 데이터로 우선 관리한다.
- 게임가이드 데이터는 우선 `guides` 테이블에 큐레이션 row로 저장한다. `guideType + targetKey + patchVersion` 조합은 하나의 가이드 항목을 식별한다.
- 프론트 API 함수는 `axiosInstance` baseURL `/api`를 기준으로 `/guide`, `/guide/{tab}`을 호출하고, 백엔드 Controller는 `/api/guide`, `/api/guide/{tab}` 경로를 제공한다.
- `GET /api/guide`는 각 탭의 대표 데이터를 반환하고, 응답은 `ApiResponse<List<GuideEntryResponse>>`를 기본 계약으로 한다.
- `GET /api/guide/{tab}`는 탭별 목록을 반환하고, 응답은 `ApiResponse<GuidePageResponse<GuideEntryResponse>>`를 기본 계약으로 한다.
- `patchVersion`이 생략된 공개 조회는 여러 패치 버전의 데이터를 섞어 반환하지 않는다. 기본 조회 기준은 최신 active 패치 또는 운영자가 current로 지정한 패치 중 하나로 확정해 적용한다.
- `{tab}` 값은 `traits`, `items`, `augments`, `champions`만 허용한다.
- 탭별 조회는 `patchVersion`, `query`, `page`, `pageSize`, `sortKey`, `sortDir`, `cost` query parameter를 지원할 수 있어야 한다.
- `cost` 필터는 챔피언 탭에서만 적용한다. 다른 탭에 전달되면 공개 API는 400 오류를 반환하지 않고 무시한다.
- `sortKey`는 `avgPlace`, `pickRate`, `top4`, `winRate`를 허용하고, `sortDir`은 `asc`, `desc`를 허용한다.
- DB `LIKE` 검색에 사용하는 `query`는 `%`, `_`, `\` 같은 와일드카드 문자를 escape해 의도하지 않은 확장 검색을 피한다.
- `{tab}`, `sortKey`, `sortDir`, `page`, `pageSize`가 허용 범위를 벗어나면 400 계열 검증 오류로 처리한다.
- 페이지 응답은 `items`, `page`, `pageSize`, `totalItems`, `totalPages`를 포함한다.
- `GuideEntryResponse`는 `id`, `guideType`, `targetKey`, `name`, `summary`, `imageUrl`, `patchVersion`, `sortOrder`, `dataJson`을 포함한다.
- `guideType`은 `TRAIT`, `ITEM`, `AUGMENT`, `CHAMPION` enum으로 관리한다.
- `dataJson`은 유형별 표시 데이터를 담는다. DB 저장 방식이 문자열 또는 JSON 컬럼이어도 공개 응답에서는 문자열이 아니라 JSON object로 직렬화한다.
  - `TRAIT`: `type`, `count`, `levels`, `champions`, `tips`, `tone`
  - `ITEM`: `category`, `winRate`, `top4`, `pickRate`, `avgPlace`, `bestUsers`, `combinations`
  - `AUGMENT`: `tier`, `type`, `description`, `reward`, `tags`, `winRate`, `pickRate`, `avgPlace`
  - `CHAMPION`: `cost`, `role`, `position`, `traits`, `bestItems`, `stats`
- 관리자 API는 `/api/admin/guides` 아래에 둔다.
- 관리자는 가이드 항목 목록 조회, 생성, 수정, 숨김 처리를 할 수 있어야 한다.
- 관리자 생성/수정 시 `dataJson`은 JSON object로 파싱 가능한지 저장 전에 검증한다.
- 관리자 생성 시 같은 `guideType + targetKey + patchVersion` 조합이 이미 있으면 새 row를 무조건 만들기보다 기존 row 수정 또는 soft delete row 복구 정책을 우선 적용한다.
- 관리자 삭제 요청은 우선 실제 삭제보다 `isActive = false` 또는 `deletedAt` 기반 soft delete로 설계한다.
- 관리자가 입력한 이미지 URL 또는 Community Dragon key는 공개 응답에서 프론트가 바로 사용할 수 있는 `imageUrl`로 제공한다.

### 5. PatchNotes

- 사용자는 최신 패치노트와 이전 패치 히스토리를 조회할 수 있어야 한다.
- 패치노트는 게임가이드와 별도 기능으로 구현하며, 패치노트 구현 PR에는 게임가이드 Entity, Controller, DTO 변경을 포함하지 않는다.
- 패치노트는 버전, 제목, 요약, 설명, 적용일, 현재 패치 여부, 대표 이미지를 가질 수 있어야 한다.
- 변경사항은 챔피언, 시너지, 아이템, 증강체, 시스템 같은 카테고리로 구분될 수 있어야 한다.
- 변경사항은 상향, 하향, 조정, 신규 같은 변경 타입을 가질 수 있어야 한다.
- 공개 API는 사용자에게 노출 가능한 패치노트와 변경사항만 반환해야 한다.
- 관리자가 나중에 패치노트와 변경사항을 수정하거나 추가할 수 있는 구조여야 한다.
- 패치노트는 대량 매치 데이터 집계 결과가 아니라 관리자 큐레이션 기반 공지/변경사항 데이터로 우선 관리한다.
- 패치노트 데이터는 우선 `patch_notes`, `patch_changes` 테이블에 큐레이션 row로 저장한다. `patch_notes.version`은 하나의 패치노트를 식별한다.
- 프론트 API 함수는 `axiosInstance` baseURL `/api`를 기준으로 `/patch-notes`, `/patch-notes/{version}/changes`를 호출하고, 백엔드 Controller는 `/api/patch-notes`, `/api/patch-notes/{version}/changes` 경로를 제공한다.
- `GET /api/patch-notes`는 패치노트 목록을 반환하고, 응답은 `ApiResponse<List<PatchNoteResponse>>`를 기본 계약으로 한다.
- `GET /api/patch-notes/{version}/changes`는 특정 버전의 변경사항 목록과 통계를 반환하고, 응답은 `ApiResponse<PatchChangePageResponse>`를 기본 계약으로 한다.
- 변경사항 조회는 `category`, `type`, `impact`, `query`, `page`, `pageSize` query parameter를 지원한다.
- `category`는 `CHAMPION`, `TRAIT`, `ITEM`, `AUGMENT`, `SYSTEM` enum으로 관리한다.
- `type`은 `BUFF`, `NERF`, `ADJUST`, `NEW` enum으로 관리한다.
- `impact`는 `HIGH`, `MEDIUM`, `LOW` enum으로 관리한다.
- 패치노트 목록은 현재 패치를 우선 노출하고, 그 다음 `publishedAt` 최신순으로 정렬한다.
- 존재하지 않는 `{version}`의 변경사항 조회는 404 계열 오류로 처리한다.
- 필터 결과가 없는 경우에는 404가 아니라 빈 `items`와 정상 페이지 메타를 반환한다.
- `GET /api/patch-notes`의 `PatchNoteResponse`는 목록 성능을 위해 전체 `changes`를 포함하지 않는다. 기본 필드는 `id`, `version`, `title`, `summary`, `description`, `focus`, `imageUrl`, `publishedAt`, `isCurrent`, `highlights`로 한다.
- 패치노트 변경사항은 `/api/patch-notes/{version}/changes`에서 조회한다. 목록에서 변경사항 개수가 필요하면 `changeCount` 같은 요약 필드만 추가한다.
- `PatchChangeResponse`는 `id`, `category`, `type`, `impact`, `targetKey`, `targetName`, `summary`, `beforeValue`, `afterValue`, `imageUrl`, `tags`를 포함한다.
- 변경사항 페이지 응답은 `items`, `page`, `pageSize`, `totalItems`, `totalPages`, `stats`를 포함한다.
- `stats`는 선택된 패치 버전 전체 변경사항 기준으로 계산하고, 현재 category/type/impact/query 필터에 의해 줄어든 페이지 결과와 구분한다.
- `stats`는 `totalChanges`, `categoryCounts`, `typeCounts`, `buffCount`, `nerfCount`, `highImpactCount`를 포함한다.
- `categoryCounts`는 `ALL`, `CHAMPION`, `TRAIT`, `ITEM`, `AUGMENT`, `SYSTEM` key를 사용할 수 있고, `typeCounts`는 `BUFF`, `NERF`, `ADJUST`, `NEW` key를 사용한다.
- 관리자 API는 `/api/admin/patch-notes`와 `/api/admin/patch-note-changes` 아래에 둔다.
- 관리자는 패치노트 목록 조회, 생성, 수정, 숨김 처리를 할 수 있어야 한다.
- 관리자는 패치노트 하위 변경사항을 생성, 수정, 숨김 처리할 수 있어야 한다.
- 관리자 생성/수정 시 `highlightsJson`, `tagsJson`은 JSON string array로 파싱 가능한지 저장 전에 검증한다.
- 관리자 생성 시 같은 `version`의 패치노트가 이미 있으면 새 row를 무조건 만들기보다 기존 row 수정 또는 soft delete row 복구 정책을 우선 적용한다.
- 관리자 삭제 요청은 우선 실제 삭제보다 `isActive = false` 또는 `deletedAt` 기반 soft delete로 설계한다.
- 현재 패치가 여러 개가 되지 않도록 `isCurrent` 변경 시 기존 현재 패치 해제 정책을 함께 적용한다.

### 6. Community

- 사용자는 파티 모집 글 목록을 조회할 수 있어야 한다.
- 사용자는 모집 글을 작성하고 상태를 변경할 수 있어야 한다.
- 모집 글은 제목, 모집 인원, 티어 조건, 게임 모드, 메모, 모집 상태를 가질 수 있어야 한다.
- 검색, 필터, 정렬, 페이지네이션을 지원할 수 있어야 한다.
- 실시간 채팅이 도입될 경우 WebSocket 또는 별도 채팅 서버와의 경계를 명확히 둔다.
- 관리자 또는 작성자는 부적절한 글을 숨김 처리할 수 있어야 한다.

### 7. AI Recommend

- 사용자는 현재 보유 유닛/아이템 정보를 기반으로 AI 추천을 요청할 수 있어야 한다.
- 백엔드는 프론트 요청을 검증하고 ai-server에 전달한다.
- 백엔드는 ai-server 응답을 프론트가 사용할 수 있는 표준 응답으로 변환한다.
- AI 서버 장애, 타임아웃, 잘못된 입력은 명확한 오류로 반환한다.
- AI 추천은 메타 덱 데이터와 별개로 동작하되, 필요하면 메타 덱 데이터를 참고 정보로 전달할 수 있어야 한다.

### 8. Admin

- 관리자는 자동 수집/집계 데이터의 품질을 보정할 수 있어야 한다.
- 관리자 기능은 공개 사용자 흐름과 분리되어야 한다.
- 관리자 API는 전체 데이터 조회, 생성, 수정, 숨김 처리 또는 삭제를 지원할 수 있어야 한다.
- 삭제 정책은 실제 삭제보다 숨김 처리 또는 soft delete를 우선 검토한다.
- 관리자 작업은 추후 감사 로그 또는 수정 이력 저장으로 확장 가능해야 한다.

---

## 데이터 관리 정책

### 자동 수집 데이터

- Riot API 기반 데이터는 원천 응답을 그대로 화면에 노출하지 않고 필요한 필드로 정규화한다.
- 중복 데이터와 품질 낮은 데이터는 집계 단계에서 최대한 제거한다.
- rate limit 때문에 충분한 표본을 확보하지 못한 경우 응답에 표본 수 또는 데이터 기준을 명확히 포함한다.

### 관리자 큐레이션 데이터

- 자동 데이터의 품질 한계를 보완하기 위해 관리자 큐레이션 레이어를 둔다.
- 관리자 큐레이션은 원천 데이터를 삭제하지 않고 공개 표시값을 덮어쓰는 방식으로 우선 설계한다.
- 공개 API는 큐레이션 데이터를 반영한 최종 표시값을 반환한다.

### 정적 데이터

- 챔피언, 아이템, 시너지, 증강체 이름과 이미지는 Community Dragon 기준으로 보강한다.
- DB에는 가능한 한 URL 원문보다 정적 데이터 식별자를 저장한다.
- 프론트가 URL을 생성할 수 있는 데이터는 key 중심 응답을 우선 검토한다.

---

## 프론트 연동 정책

- 프론트 `axiosInstance` 기본 baseURL은 `/api`다.
- 프론트 API 함수에서 `/api/api/...` 중복 경로가 생기지 않도록 백엔드 경로와 호출 경로를 함께 검토한다.
- API 응답 필드명은 camelCase를 기본으로 한다.
- 프론트가 이미 사용하는 화면 계약이 있다면 백엔드 DTO 작성 전에 반드시 대조한다.
- fallback 데이터가 있는 화면은 실제 API 전환 시 fallback 유지/제거 정책을 별도로 결정한다.
