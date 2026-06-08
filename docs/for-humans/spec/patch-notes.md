# PatchNotes — 패치 노트

## 개요

최신 패치노트와 이전 패치 히스토리를 한 화면에서 확인하는 페이지.
관리자 큐레이션 기반 공지/변경사항 데이터로 관리한다.

---

## 기능 목록

- 최신 패치 노트를 기본으로 표시한다
- 히스토리 영역에서 이전 패치를 클릭하면 해당 버전 정보로 전환된다
  - 활성 히스토리 항목은 active 상태로 표시
  - 패치 전환 시 요약·카운트·변경 목록이 함께 갱신된다

### 패치 요약 영역

- 패치 버전, 적용일, 설명, 전체 변경 건수, 핵심 영향 변경 건수, 상향/하향 항목 수
- 현재 선택 패치의 대표 이미지 (CDragon CDN 또는 fallback 이미지)

### 변경사항 목록

- 카테고리별: 챔피언 / 시너지 / 아이템 / 증강체 / 시스템
- 변경 유형: 상향(BUFF) / 하향(NERF) / 조정(ADJUST) / 신규(NEW)
- 각 항목: 카테고리, 변경 유형, 영향도(높음/중간/낮음), 이름, 요약, 태그, 이미지
- "상세 보기" 클릭 시 이전/변경 수치 비교 표시 (`aria-expanded`)
- 변경사항은 5개씩 페이지네이션

### 검색/필터

- 키워드 검색, 카테고리 필터, 변경 유형 필터, 영향 높음만 보기 필터
- 검색어/필터 변경 시 페이지는 1페이지로 초기화

---

## API 계약

### 백엔드 엔드포인트

- `GET /api/patch-notes` — 패치 버전 목록 + 요약 (`ApiResponse<List<PatchNoteResponse>>`)
- `GET /api/patch-notes/{version}/changes` — 변경사항 목록 + 통계 (`ApiResponse<PatchChangePageResponse>`)
  - 존재하지 않는 버전: 404. 필터 결과 없음: 빈 items + 정상 페이지 메타

### 쿼리 파라미터 (changes)

| 파라미터 | 값 |
|---------|-----|
| `category` | `CHAMPION` / `TRAIT` / `ITEM` / `AUGMENT` / `SYSTEM` (생략=전체) |
| `type` | `BUFF` / `NERF` / `ADJUST` / `NEW` (생략=전체) |
| `impact` | `HIGH` / `MEDIUM` / `LOW` (생략=전체) |
| `query` | 키워드 |
| `page` | 1부터 시작 |
| `pageSize` | 기본 5 |

### PatchNoteResponse 필드

`id`, `version`, `title`, `summary`, `description`, `focus`, `imageUrl`, `publishedAt`, `isCurrent`, `highlights`
- 목록 성능을 위해 전체 `changes`는 포함하지 않음

### PatchChangePageResponse 구조

```json
{
  "items": [],
  "page": 1,
  "pageSize": 5,
  "totalItems": 23,
  "totalPages": 5,
  "stats": {
    "totalChanges": 23,
    "categoryCounts": { "CHAMPION": 10, "TRAIT": 4, "ITEM": 3, "AUGMENT": 2, "SYSTEM": 4 },
    "typeCounts": { "BUFF": 8, "NERF": 6, "ADJUST": 7, "NEW": 2 },
    "buffCount": 8,
    "nerfCount": 6,
    "highImpactCount": 4
  }
}
```

- `stats`는 현재 필터가 아닌 해당 패치 버전 **전체** 기준으로 계산
- `items` 대신 `content` 허용

### PatchChangeResponse 필드

`id`, `category`, `type`, `impact`, `targetKey`, `targetName`, `summary`, `beforeValue`, `afterValue`, `imageUrl`, `tags`

---

## 프론트 구조

- `pages/PatchNotes/PatchNotes.tsx` — 페이지 상태 관리 + 컴포넌트 조립
- `pages/PatchNotes/components/`
  - `PatchHero` / `PatchStatusBanner` / `PatchSummaryGrid`
  - `PatchChangeFilters` / `PatchChangeList` / `PatchPagination` / `PatchSideRail`
- `pages/PatchNotes/patchNotesImages.ts` — CDragon CDN 이미지 매핑 + fallback
- `mocks/patchNotesMock.ts` — API 미연동/오류 시 fallback 샘플 데이터
- `api/patchNotes.ts` + `hooks/usePatchNotes.ts` — HTTP + TanStack Query

---

## 데이터 정책

- 패치노트는 관리자 큐레이션 기반 (`/api/admin/patch-notes`, `/api/admin/patch-note-changes`)
- 로컬 DB smoke 기준 테이블은 `patch_notes`, `patch_changes`이며, `patch_notes.version`을 하나의 패치노트 식별자로 본다
- `isCurrent` 변경 시 기존 현재 패치 자동 해제
- 관리자 생성/수정 시 `highlightsJson`, `tagsJson`은 JSON string array로 검증한다
- 같은 `version` 생성 요청은 새 row를 무조건 만들기보다 기존 row 수정 또는 soft delete row 복구 정책을 우선 적용한다
- 관리자 삭제: 실제 삭제보다 `isActive = false` 또는 `deletedAt` 기반 soft delete 우선

---

## 접근성 요구사항

- 히스토리 선택 버튼: `aria-pressed` / 페이지네이션: `aria-current` / 상세 보기: `aria-expanded`
- 패치 버전/검색어/필터 변경 시 현재 페이지 + 펼쳐진 상세 비교 상태 초기화
- API 오류 시 에러 메시지 + 재시도 버튼 제공
