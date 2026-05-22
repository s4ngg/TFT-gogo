# constitution.md — 프로젝트 규칙 정의

> 어떻게 만드는가. 팀 전체가 따르는 개발 프로세스와 컨벤션을 정의한다.

---

## 기술 스택

| 구분 | 패키지 | 버전 |
|------|--------|------|
| 프레임워크 | react / react-dom | ^18.3.1 |
| 언어 | typescript | ^5.4.5 |
| 빌드 도구 | vite | ^5.3.1 |
| 라우팅 | react-router-dom | ^6.23.1 |
| 상태관리 | zustand | ^4.5.2 |
| 서버 상태 | @tanstack/react-query | ^5.40.0 |
| HTTP 클라이언트 | axios | ^1.7.2 |

```json
"scripts": {
  "dev":     "vite",
  "build":   "tsc && vite build",
  "preview": "vite preview",
  "lint":    "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0"
}
```

- **스타일**: CSS Modules (`PageName.module.css`) — Tailwind 사용 금지
- **폰트**: Pretendard (CDN)

---

## 폴더 구조 규칙

```
src/
  api/
    axiosInstance.ts
    communityDragonAssets.ts   # Community Dragon URL helper
  components/
    common/
      ChampionCard/
      TierBadge/
      TraitHexBadge/
    layout/
      AppLayout.tsx            # 사이드바 + TopBar 공통 shell
      Sidebar.tsx
      TopBar.tsx
      README.md
  hooks/
  pages/
    Dashboard/
    SummonerDetail/
    Decks/
    AiRecommend/
    Guide/
    Community/
    PatchNotes/
  store/
  styles/
    global.css
    variables.css              # 디자인 토큰 CSS 변수
```

- 페이지 전용 컴포넌트는 `pages/<PageName>/components/` 안에 위치
- 2개 이상 페이지에서 쓰이면 `components/common/`으로 이동
- 새 공통 컴포넌트 추가 시 `components/layout/README.md`에 기록

---

## 디자인 토큰 (CSS 변수)

`src/styles/variables.css` 기준으로 관리. 컴포넌트 내부에 색상/크기 하드코딩 금지.

```css
/* 배경 */
--bg-main:      #070d14;
--bg-sidebar:   #050a10;
--bg-card:      #0b1420;
--bg-card-soft: #101a27;

/* 테두리 */
--border:        #1f2a37;
--border-active: #00d4b4;

/* 포인트 색상 */
--color-cyan:    #05f3e7;   /* 활성 아이콘, CTA */
--color-cyan-num:#04ede0;   /* 승률 강조 숫자 */
--color-gold:    #f7d26d;   /* 랭크 1~2, 티어 배지 */
--color-red:     #ff4545;   /* 마감 시간 경고 */

/* 레이아웃 */
--sidebar-width: 222px;
--card-radius:   12px;
```

---

## 타이포그래피 규칙

```css
font-family: "Pretendard", -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
font-feature-settings: "tnum";
font-variant-numeric: tabular-nums;
```

| 용도 | 크기 | 굵기 | 자간 |
|------|-----:|-----:|-----:|
| 로고 텍스트 | 25px | 800 | -0.04em |
| 메인 검색 제목 | 25px | 600 | 0.045em |
| 섹션 제목 | 21px | 600 | -0.024em |
| 메뉴 텍스트 | 16px | 500 | -0.015em |
| 덱명 | 15px | 600 | -0.015em |
| 버튼 텍스트 | 14px | 600 | -0.01em |
| 본문 설명 | 13px | 400~500 | -0.01em |

- 제목·버튼·탭·덱명은 `white-space: nowrap` 우선
- 넘치는 텍스트: `overflow: hidden` + `text-overflow: ellipsis`
- 한국어 UI: `word-break: keep-all`
- 숫자: `tabular-nums` 적용 (폭 고정)

---

## 에셋 정책

| 분류 | 방식 |
|------|------|
| TFTgogo 로고, 랭크 엠블럼, 패치 카드 장식 | `public/assets/`에 직접 저장 |
| 챔피언·아이템·시너지·증강체 이미지 | Community Dragon CDN |
| 파티원 모집 유형 아이콘 | CSS + lucide 아이콘 조합 |

**직접 저장 파일 경로** (`frontend/public/assets/`)

| 파일 | 앱 경로 | 사용 위치 |
|------|---------|-----------|
| `brand/tftgogo-mark.png` | `/assets/brand/tftgogo-mark.png` | 사이드바 로고 아이콘 |
| `illustrations/riot-api-mascot-card.png` | `/assets/illustrations/riot-api-mascot-card.png` | Riot API 연동 카드 |
| `emblems/patch-meta-emblem-pink.png` | `/assets/emblems/patch-meta-emblem-pink.png` | 추천 메타 카드 엠블럼 |
| `ranks/platinum-emblem.png` | `/assets/ranks/platinum-emblem.png` | 사이드바 랭크 카드 |

- CDN URL은 `api/communityDragonAssets.ts`에서 중앙 관리
- 컴포넌트 내부에 이미지 URL 하드코딩 금지
- 커스텀 이미지 추가 시 이 문서와 `frontend/public/assets/README.md`에 기록

---

## 코딩 컨벤션

- 파일명: 컴포넌트 `PascalCase.tsx`, 훅/유틸 `camelCase.ts`
- 컴포넌트는 `export default` 사용
- 타입은 `interface` 우선, 유니온/인터섹션만 `type` 사용
- `any` 사용 금지 → `unknown` 또는 명시적 타입
- 함수형 컴포넌트만 사용 (클래스 컴포넌트 금지)
- 모든 `async` 함수는 `try/catch` 처리 필수

---

## PR 규칙

- **1 PR = 1 기능 단위** — 기능이 크면 UI / API 연동으로 나눠서 올린다
- **파일 수 권고: 20개 이하** — 초과 시 작업 단위를 나눌 수 있는지 먼저 검토
- **예외 허용** — 파일 수가 넘더라도 기능이 쪼개지지 않는다면 허용 (단, PR 설명에 이유 명시 필수)
- **제목 형식**: `[feat] 소환사 검색 페이지 UI`, `[fix] 빈 결과 상태 처리`
  - 타입: `feat` / `fix` / `refactor` / `docs` / `chore` / `style`
- **Labels 필수** — PR 올릴 때 반드시 label 붙이기
  - 예: `feat`, `fix`, `refactor`, `docs`, `chore`
- **Milestone 필수** — 작업이 속한 마일스톤 지정 후 PR 올리기
  - 예: `Phase 1 - 기반 세팅`, `Phase 2 - 페이지 구현`
- **셀프 머지 금지** — 최소 1명 리뷰 후 머지
- **리뷰어 응답**: 24시간 내 원칙
- **main 브랜치 직접 push 금지**

---

## 브랜치 전략

```
main            — 배포용 (직접 push 금지)
develop         — 통합 브랜치
feat/기능명      — 기능 개발  (예: feat/match-search-ui)
fix/버그명       — 버그 수정  (예: fix/empty-state-crash)
```

- 작업 시작 전 `develop`에서 브랜치 생성
- 완료 후 `develop`으로 PR

---

## 커밋 메시지 규칙

```
feat: 소환사 검색 UI 컴포넌트 추가
fix: 검색 결과 없을 때 빈 화면 처리
refactor: SearchBar 컴포넌트 분리
docs: constitution.md 디자인 토큰 추가
```

- 한 커밋 = 하나의 논리적 변경
- 한글 사용 허용

---

## 라우팅 규칙

- 라우팅 초기 세팅은 1명이 담당하고 develop 머지 후 작업 시작
- 라우팅 구조 변경 시 팀 전체에 공유 후 진행
- 각자 자기 페이지 폴더만 건드리는 것을 원칙으로 한다

---

## 테스트 규칙

- 신규 API 함수에는 단위 테스트 작성 필수
- PR 머지 전 `pnpm lint` 통과 필수
