# TFTgogo 관리자 페이지 스펙

v1.0 (2026-06-08)

---

## 1. 개요

TFTgogo 서비스의 콘텐츠를 관리하는 관리자 전용 웹 페이지.
일반 사용자 화면(사이드바, 헤더 등)과 완전히 분리된 독립 레이아웃으로 구성한다.

### 1.1 핵심 원칙

- 관리자 페이지는 일반 Layout(좌측 사이드바, 상단 헤더)을 사용하지 않는다.
- `/admin/*` 경로는 AdminLayout을 별도로 적용한다.
- 인증은 X-Admin-Token 기반으로 처리한다. 토큰 미보유 시 `/admin` 로그인 화면으로 리다이렉트.
- 팀원 작업 미완성 기능은 "준비 중" 화면으로 표시하고, 연동 완료 후 교체한다.

---

## 2. 라우트 구조

| 경로 | 페이지 | 상태 |
|---|---|---|
| `/admin` | 로그인 (토큰 입력) | ✅ 구현 |
| `/admin/decks` | 메타덱 관리 | ✅ 구현 (백엔드 연동 필요) |
| `/admin/hero-augments` | 영웅증강 덱 관리 | ✅ 구현 (백엔드 연동 필요) |
| `/admin/guides` | 게임가이드 관리 | 🔧 프론트 작성 필요 |
| `/admin/patch-notes` | 패치노트 관리 | 🔧 프론트 작성 필요 |
| `/admin/members` | 회원 관리 | ⏳ 준비 중 (팀원 연동 대기) |
| `/admin/community` | 커뮤니티 관리 | ⏳ 준비 중 (팀원 연동 대기) |

---

## 3. 레이아웃

### 3.1 AdminLayout

```
┌─────────────────────────────────────────────┐
│  Admin 전용 사이드바 (좌, 고정)               │
│  ┌──────────┐  ┌───────────────────────────┐ │
│  │ TFTgogo  │  │                           │ │
│  │ 관리자   │  │      콘텐츠 영역           │ │
│  │──────────│  │                           │ │
│  │ 메타덱   │  │                           │ │
│  │ 영웅증강 │  │                           │ │
│  │ 가이드   │  │                           │ │
│  │ 패치노트 │  │                           │ │
│  │ 회원     │  │                           │ │
│  │ 커뮤니티 │  │                           │ │
│  │──────────│  │                           │ │
│  │ 로그아웃 │  │                           │ │
│  └──────────┘  └───────────────────────────┘ │
└─────────────────────────────────────────────┘
```

### 3.2 파일 구조

```
frontend/src/
├── layouts/
│   └── AdminLayout.tsx          ← 신규 (사이드바 + 콘텐츠 영역)
├── pages/Admin/
│   ├── AdminLogin.tsx           ← 기존 TokenGate 로직 이관
│   ├── AdminDecks.tsx           ← 기존 메타덱 탭 분리
│   ├── AdminHeroAugments.tsx    ← 기존 영웅증강 탭 분리
│   ├── AdminGuides.tsx          ← 신규
│   ├── AdminPatchNotes.tsx      ← 신규
│   ├── AdminMembers.tsx         ← 준비 중 화면
│   ├── AdminCommunity.tsx       ← 준비 중 화면
│   └── Admin.module.css         ← 기존 유지
└── components/admin/
    └── AdminSidebar.tsx         ← 신규
```

---

## 4. 인증

- 토큰 입력 → `localStorage`에 `tftgogo_admin_token` 저장
- 모든 Admin API 요청 시 `X-Admin-Token` 헤더에 포함
- 토큰 미보유 또는 401 응답 시 `/admin` 로그인 화면으로 리다이렉트
- 로그아웃 시 `localStorage`에서 토큰 제거 후 `/admin` 이동

---

## 5. 기능별 스펙

### 5.1 메타덱 관리 (`/admin/decks`)

**기존 구현 유지. 별도 페이지로 분리만 진행.**

| 기능 | API | 상태 |
|---|---|---|
| 덱 목록 조회 (랭크 필터) | `GET /api/admin/decks?rankFilter=` | ✅ |
| 덱 큐레이션 저장 | `PATCH /api/admin/decks/{deckId}` | ✅ |
| 덱 큐레이션 초기화 | `DELETE /api/admin/decks/{deckId}/curation` | ✅ |
| 배치판 편집 모달 | (boardPositions 필드) | ✅ |
| 운영방법 편집 모달 | (playGuide 필드) | ✅ |
| 영웅증강 편집 모달 | (heroAugments 필드) | ✅ |

---

### 5.2 영웅증강 덱 관리 (`/admin/hero-augments`)

**프론트 구현 완료. 백엔드 API 연동 필요.**

| 기능 | API | 상태 |
|---|---|---|
| 덱 목록 조회 | `GET /api/admin/hero-augment-decks` | 백엔드 미구현 |
| 덱 생성 | `POST /api/admin/hero-augment-decks` | 백엔드 미구현 |
| 덱 수정 | `PUT /api/admin/hero-augment-decks/{id}` | 백엔드 미구현 |
| 덱 삭제 | `DELETE /api/admin/hero-augment-decks/{id}` | 백엔드 미구현 |

**요청/응답 DTO:**

```json
// HeroAugmentDeckPayload (요청)
{
  "name": "string",
  "description": "string | null",
  "champions": "JSON string | null",
  "traits": "JSON string | null",
  "boardPositions": "JSON string | null",
  "heroAugments": "JSON string | null",
  "recommended": true,
  "sortOrder": 0,
  "grade": "S | A+ | A | B | C | D | null"
}

// HeroAugmentDeckItem (응답)
{
  "id": 1,
  "name": "string",
  "description": "string | null",
  "champions": "JSON string | null",
  "traits": "JSON string | null",
  "boardPositions": "JSON string | null",
  "heroAugments": "JSON string | null",
  "recommended": true,
  "sortOrder": 0,
  "grade": "string | null"
}
```

---

### 5.3 게임가이드 관리 (`/admin/guides`)

**백엔드 완성. 프론트 UI 작성 필요.**

| 기능 | API |
|---|---|
| 가이드 목록 조회 | `GET /api/admin/guides` |
| 가이드 생성 | `POST /api/admin/guides` |
| 가이드 수정 | `PATCH /api/admin/guides/{guideId}` |
| 가이드 삭제 (소프트) | `DELETE /api/admin/guides/{guideId}` |

**UI 구성:**
- 가이드 목록 테이블 (guideType, targetKey, patchVersion, active 표시)
- 생성/수정 모달 (guideType 선택, targetKey 입력, patchVersion 입력, dataJson 편집)
- 삭제 확인 다이얼로그
- active 토글로 공개/비공개 전환

**비즈니스 룰:**
- `dataJson`은 유효한 JSON 객체여야 함
- `active=false`이면 일반 사용자에게 노출 안 됨
- 소프트삭제 — 실제 행 제거 없음

---

### 5.4 패치노트 관리 (`/admin/patch-notes`)

**백엔드 완성. 프론트 UI 작성 필요.**

| 기능 | API |
|---|---|
| 패치노트 목록 조회 | `GET /api/admin/patch-notes` |
| 패치노트 생성 | `POST /api/admin/patch-notes` |
| 패치노트 수정 | `PATCH /api/admin/patch-notes/{patchNoteId}` |
| 패치노트 삭제 (소프트) | `DELETE /api/admin/patch-notes/{patchNoteId}` |
| 패치 변경사항 생성 | `POST /api/admin/patch-note-changes` |
| 패치 변경사항 수정 | `PATCH /api/admin/patch-note-changes/{changeId}` |
| 패치 변경사항 삭제 (소프트) | `DELETE /api/admin/patch-note-changes/{changeId}` |

**UI 구성:**
- 패치노트 목록 (version, title, isCurrent 표시)
- 패치노트 생성/수정 모달
  - version, title, summary, isCurrent 체크박스
  - highlightsJson, tagsJson 입력 (JSON 배열)
- 패치 변경사항 편집 (패치노트 상세 내 인라인)
  - championKey, changeType, description 입력

**비즈니스 룰:**
- `isCurrent=true`는 전체에서 하나만 유지 (중복 시 에러)
- `highlightsJson`, `tagsJson`은 JSON 문자열 배열이어야 함
- 소프트삭제 — 실제 행 제거 없음

---

### 5.5 회원 관리 (`/admin/members`) — 준비 중

팀원 백엔드 작업 완료 후 연동.  
현재는 "준비 중입니다." 화면 표시.

---

### 5.6 커뮤니티 관리 (`/admin/community`) — 준비 중

팀원 백엔드 작업 완료 후 연동.  
현재는 "준비 중입니다." 화면 표시.

---

## 6. 공통 UI 규칙

- 저장 중 버튼 비활성화 + 로딩 텍스트 표시
- API 오류 시 인라인 에러 메시지 표시 (모달 내부)
- 삭제 전 `confirm()` 다이얼로그 표시
- 목록 조회 실패 시 빈 화면 + 에러 메시지

---

## 7. 구현 순서

1. `AdminLayout` + `AdminSidebar` 신규 생성
2. 기존 `Admin.tsx` 기능을 페이지별로 분리
3. 라우터에서 `/admin/*` → AdminLayout 적용
4. `AdminGuides.tsx` 작성
5. `AdminPatchNotes.tsx` 작성
6. 회원/커뮤니티 준비 중 페이지 작성
