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
- [ ] 각 페이지 빈 컴포넌트 생성 (Dashboard, SummonerDetail, Decks, AiRecommend, Guide, Community, PatchNotes)
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

### PR 6: SummonerDetail UI
- [ ] `MatchList.tsx` — 최근 전적 목록
- [ ] `MatchItem.tsx` — 전적 항목 (순위, TOP4/BOT 구분, 덱 이름, 게임 시간, LP 변동, 유닛 이미지)
- [ ] `UpdateButton.tsx` — 전적 업데이트 버튼
- [ ] 로딩 / 빈 상태 / 에러 상태 UI

### PR 7: SummonerDetail API 연동
- [ ] `api/match.ts`
- [ ] `hooks/useSummonerDetail.ts`
- [ ] `hooks/useMatchHistory.ts`
- [ ] PR 6 컴포넌트에 실제 데이터 연결

### PR 8: Decks UI + API 연동
- [ ] 덱 목록 UI 컴포넌트
- [ ] 덱 상세 UI 컴포넌트
- [ ] `api/decks.ts`
- [ ] `hooks/useDecks.ts`

### PR 9: AiRecommend UI + API 연동
- [ ] AI 추천 UI 컴포넌트
- [ ] `api/aiRecommend.ts`
- [ ] `hooks/useAiRecommend.ts`

### PR 10: Guide UI + API 연동
- [ ] 유닛/시너지/아이템 조회 UI
- [ ] API 연동

### PR 11: Community UI + API 연동
- [ ] 파티원 모집글 목록/필터/검색 UI
- [ ] 모집글 작성 폼 UI (제목, 랭크 유형, 최소 티어, 모집 인원, 플레이 스타일)
- [ ] 실시간 채팅 채널 목록 UI (#일반, #덱 공략, #파티 모집, #질문 & 답변)
- [ ] 채팅 메시지 영역 UI (온라인 수, 실시간 메시지)
- [ ] API 연동 (모집글 CRUD, 채팅 WebSocket)

### PR 12: PatchNotes UI + API 연동
- [ ] 최신 패치 노트 UI
- [ ] 챔피언/아이템/시너지 변경사항 UI
- [ ] 패치 히스토리 목록 UI
- [ ] API 연동

---

## Phase 3 — 마무리

### PR 13: 공통 마무리
- [ ] 전체 페이지 반응형 점검
- [ ] 에러/빈 상태 누락 항목 보완
- [ ] checklist.md 전체 항목 최종 확인
