# tasks.md — 순서 정의

> 어떤 순서로 만드는가. 작업 순서와 PR 단위를 정의한다.
> 체크박스는 완료 시 체크한다.

---

## Phase 1 — 기반 세팅 (1명 담당)

### PR 1: 프로젝트 초기 세팅
- [ ] Vite + React + TypeScript 초기화 확인
- [ ] 폴더 구조 생성
- [ ] ESLint, 절대경로(`@/`) 설정
- [ ] `styles/global.css` — reset, Pretendard 폰트 import
- [ ] `styles/variables.css` — 디자인 토큰 CSS 변수 작성
- [ ] `api/axiosInstance.ts` — Axios 기본 설정
- [ ] `api/communityDragonAssets.ts` — CDN URL helper 기본 작성

### PR 2: 공통 레이아웃 + 라우팅 세팅 ← **내가 담당**
- [ ] `components/layout/Sidebar.tsx` — 로고, 메뉴, 랭크 카드, 의견 보내기
- [ ] `components/layout/TopBar.tsx` — 알림, 메일, 도움말, 프로필
- [ ] `components/layout/AppLayout.tsx` — Sidebar + TopBar + Outlet 조합
- [ ] `components/layout/README.md` — 공통 컴포넌트 목록 기록
- [ ] `App.tsx` — React Router v6 라우팅 설정
- [ ] 각 페이지 빈 컴포넌트 생성 (Dashboard, Decks, AiRecommend, Guide, Party, PatchNotes)
- [ ] `pages/NotFound.tsx` — 404 페이지

> ✅ PR 2 develop 머지 완료 후 팀원 각자 페이지 작업 시작

---

## Phase 2 — 공통 컴포넌트 + 페이지 구현 (병렬 작업 가능)

### PR 3: 공통 컴포넌트
- [ ] `components/common/TierBadge/` — S, A+, A 등급 배지
- [ ] `components/common/ChampionCard/` — 챔피언 이미지, 별 등급, 아이템 슬롯
- [ ] `components/common/TraitHexBadge/` — 시너지/특성 육각 배지

### PR 4: Dashboard UI
- [ ] `SearchBar.tsx` — 검색 입력창, 버튼, 최근 검색 태그
- [ ] `SummonerCard.tsx` — 소환사 정보 카드 (티어, LP, 승률)
- [ ] `MetaSnapshot.tsx` — 메타 스냅샷 테이블
- [ ] `MetaTabs.tsx` — 종합/상위권/마스터+ 탭
- [ ] 로딩 / 빈 상태 / 에러 상태 UI

### PR 5: Dashboard API 연동
- [ ] `api/summoner.ts`
- [ ] `api/meta.ts`
- [ ] `hooks/useSummoner.ts`
- [ ] `hooks/useMetaSnapshot.ts`
- [ ] PR 4 컴포넌트에 실제 데이터 연결

### PR 6: Decks UI + API 연동
- [x] 덱 목록 UI (`pages/Decks/`) — 티어 그룹핑, 랭크 필터, 챔피언·시너지·통계 표시
- [x] 덱 상세 UI (`pages/DeckDetail/`) — 헥스 보드, 레벨 탭, 시너지, 추천 아이템
- [x] 배치판 자동 배치 로직 (CDragon range/role 기반, `buildBoardPositions`)
- [x] 배치판 관리자 배치 우선 적용 (`boardPositions` JSON → 자동 배치 폴백)
- [x] `api/deckApi.ts` — 메타 덱 목록/상세 API
- [x] `hooks/useMetaSnapshot.ts`, `hooks/useDeckQuery.ts` — React Query 연동
- [ ] 증강 추천 — 관리자 수동 큐레이션 UI/저장 (향후 작업)

### PR 7: AiRecommend UI + API 연동
- [ ] AI 추천 UI 컴포넌트
- [ ] `api/aiRecommend.ts`
- [ ] `hooks/useAiRecommend.ts`

### PR 8: Guide UI + API 연동
- [ ] 유닛/시너지/아이템 조회 UI
- [ ] API 연동

### PR 9: Party UI + API 연동
- [ ] 파티원 모집 목록/작성 UI (유형 아이콘: CSS + lucide)
- [ ] 채팅 채널 목록 UI
- [ ] API 연동

### PR 10: PatchNotes UI + API 연동
- [ ] 최신 패치 노트 UI
- [ ] 챔피언/아이템/시너지 변경사항 UI
- [ ] 패치 히스토리 목록 UI
- [ ] API 연동

---

## Phase 3 — 마무리

### PR 11: 공통 마무리
- [ ] 전체 페이지 반응형 점검
- [ ] 에러/빈 상태 누락 항목 보완
- [ ] checklist.md 전체 항목 최종 확인
