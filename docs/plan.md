# plan.md — 기술 정의

> 어떤 기술로 만드는가. 기술 설계와 구조를 정의한다.

---

## 라우팅 구조

```
/                             → Dashboard (기본 페이지)
/decks                        → Decks (덱 목록)
/decks/:deckId                → DeckDetail (덱 상세)
/ai-recommend                 → AiRecommend (AI 덱 추천)
/guide                        → Guide (게임 가이드)
/party                        → Party (파티원 찾기)
/patch-notes                  → PatchNotes (패치 노트)
/summoner/:gameName/:tagLine  → SummonerDetail (소환사 전적)
/admin                        → Admin (관리자 덱 큐레이션)
*                             → / 로 리다이렉트
```

- `App.tsx`에서 React Router v6 기준으로 라우터 설정
- **라우팅 초기 세팅은 내가 담당**, develop 머지 후 팀원 작업 시작
- 공통 레이아웃(`AppLayout`)으로 감싸서 모든 페이지에 적용

```tsx
// App.tsx 구조
<BrowserRouter>
  <Routes>
    <Route element={<AppLayout />}>
      <Route path="/"             element={<Dashboard />} />
      <Route path="/decks"        element={<Decks />} />
      <Route path="/ai-recommend" element={<AiRecommend />} />
      <Route path="/guide"        element={<Guide />} />
      <Route path="/party"        element={<Party />} />
      <Route path="/patch-notes"  element={<PatchNotes />} />
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
</BrowserRouter>
```

---

## 공통 레이아웃 구조

```
src/components/layout/
  AppLayout.tsx    # Sidebar + TopBar + <Outlet /> 조합
  Sidebar.tsx      # 로고, 메뉴, 랭크 카드, 전적 업데이트, 의견 보내기
  TopBar.tsx       # 알림, 메일, 도움말, 프로필
  README.md        # 공통 컴포넌트 추가 시 여기에 기록
```

- 사이드바 폭: `222px` 고정
- 전체 배경: `--bg-main(#070d14)`, 사이드바: `--bg-sidebar(#050a10)`

---

## 공통 컴포넌트

```
src/components/common/
  ChampionCard/      # 챔피언 이미지, 별 등급, 추천 아이템 슬롯
  TierBadge/         # S, A+, A 메타 등급 배지
  TraitHexBadge/     # 시너지/특성 육각 배지
```

- 게임 이미지(챔피언·아이템·시너지)는 Community Dragon CDN 사용
- CDN URL은 `api/communityDragonAssets.ts`에서 중앙 관리

---

## API 호출 구조

```
src/api/
  axiosInstance.ts            # Axios 기본 설정 (baseURL, 인터셉터)
  communityDragonAssets.ts    # Community Dragon URL helper (tftItemIconUrl 등)
  cdragonLocale.ts            # CDragon ko_kr.json 파싱 (챔피언/트레이트/아이템 한글화)
  summoner.ts                 # 소환사 검색 API
  meta.ts                     # 메타 스냅샷 API
  deck.ts                     # 덱 추천 API
  adminApi.ts                 # 관리자 큐레이션 API (X-Admin-Token 인증)

src/utils/
  deckUtils.ts                # costLimitForLevel 등 덱 관련 공유 유틸
```

- 모든 API 함수는 `axiosInstance` 기반으로 작성
- React Query `useQuery` / `useMutation`과 연결
- CDragon 데이터: 챔피언별 코스트/레인지/트레이트 정보를 한글화 + 배치판에 활용
- 관리자 API: `X-Admin-Token` 헤더 필수 (추후 JWT ROLE_ADMIN으로 전환)

### 증강 데이터 정책

Riot TFT-MATCH-V1 API는 `augments` 필드를 제공하지 않음 (2025년 기준 실 응답 검증).
자동 집계 불가 → 향후 관리자 수동 큐레이션(DeckCuration 확장)으로 제공 예정.

### 배치판 포지션 정책

- **자동 배치**: CDragon `range`/`role` 데이터 기반으로 프론트에서 계산
  - frontline(range ≤ 2 or tank/fighter role) → 행 2~3
  - backline(range ≥ 4 or caster/marksman role) → 행 0~1
- **관리자 배치**: `DeckCuration.boardPositions` JSON(`{"imageUrl": {"row":N,"col":N}}`) 우선 적용
  - 저장되어 있으면 자동 배치 로직을 건너뜀
  - 미지정(`null`) 시 자동 배치 폴백

---

## 상태관리 분리 기준

| 상태 종류 | 관리 방법 |
|-----------|-----------|
| 서버 데이터 (API 응답) | React Query |
| 전역 UI 상태 (로그인 소환사 등) | Zustand |
| 로컬 UI 상태 (입력값, 탭 등) | useState |

---

## 스타일 구조

```
src/styles/
  global.css      # reset, Pretendard 폰트 import
  variables.css   # 디자인 토큰 CSS 변수 (색상, 간격, radius)
```

- 페이지 전용 스타일: `pages/<PageName>/<PageName>.module.css`
- Tailwind 사용 금지
- 홈 대시보드 본문 기준 폭: `1307px`
- 카드 간격: `12~15px` 유지
