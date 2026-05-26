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
- [x] 소환사 카드 (티어, LP, 승수, 패수, 승률, 평균 순위, TOP4율, 순위 분포 바)
- [x] 많이 플레이한 시너지 / 챔피언 통계 섹션
- [x] 최근 30게임 요약 (순방확률 도넛 차트, W/L, 평균 순위)
- [x] 전적 목록 (순위 1위~8위, 덱 이름, 시간, LP 변동, 유닛+아이템 이미지)
- [x] 챔피언·아이템 이미지 호버 툴팁
- [x] 전적 행 클릭 → 8명 상세 패널 펼치기 (시너지, 증강, 챔피언, 킬수, 잔여골드)
- [x] 30개씩 더보기 버튼
- [x] 전적 업데이트 버튼 (UI)
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
- [ ] 게임 가이드 탭 UI 구성 (시너지 / 아이템 / 증강체 / 챔피언)
- [ ] 시너지 가이드 카드 UI (설명, 활성 단계, 필요 챔피언, 운영 팁)
- [ ] 아이템 통계 테이블 UI (승률, TOP4, 평균 등수, 픽률, 추천 챔피언, 3신기)
- [ ] 아이템/증강체 통계 정렬 기능
- [ ] 증강체 티어표, 보상표, 선택 플랜/배치툴 UI
- [ ] 챔피언 가이드 UI (비용 필터, hover 스탯, 클릭 상세 모달, 3신기)
- [ ] 탭별 검색 기능
- [ ] 탭별 페이지네이션 적용
- [ ] 챔피언 즐겨찾기 및 최근 본 가이드 빠른 이동
- [ ] 시너지/아이템/챔피언 간 탭 이동 UX
- [ ] Community Dragon JSON 데이터 매핑
- [ ] Community Dragon CDN 이미지 연결
- [ ] Match API 기반 아이템 통계 집계 연동

### PR 11: Community UI + API 연동
- [ ] 파티원 모집글 목록/필터/검색 UI
- [ ] 모집글 작성 폼 UI (제목, 랭크 유형, 최소 티어, 모집 인원, 플레이 스타일)
- [ ] 실시간 채팅 채널 목록 UI (#일반, #덱 공략, #파티 모집, #질문 & 답변)
- [ ] 채팅 메시지 영역 UI (온라인 수, 실시간 메시지)
- [ ] API 연동 (모집글 CRUD, 채팅 WebSocket)

### PR 12: PatchNotes UI + API 연동
- [x] 최신 패치 요약 히어로 영역 구성 (패치 버전, 적용일, 변경 건수, 핵심 영향 건수)
- [x] 현재 선택된 패치 대표 이미지 표시
- [x] 대표 이미지 로딩 실패 시 기본 패치 엠블럼 fallback 처리
- [x] 상향/하향/핵심 메타/운영 구간 요약 카드 UI 구성
- [x] 패치 변경사항 카테고리 탭 구성 (전체, 챔피언, 시너지, 아이템, 증강체, 시스템)
- [x] 패치 변경사항 검색 UI 구성 (챔피언, 아이템, 키워드 기반 필터)
- [x] 영향 높음만 보기 토글 UI 구성
- [x] 변경 타입 필터 UI 구성 (전체 변경, 상향, 하향, 조정, 신규)
- [x] 한 페이지 5개 단위 변경사항 페이지네이션 적용
- [x] 페이지 번호는 한 렌더링에 최대 5개만 노출하고 이후는 더보기로 이동
- [x] 카테고리별 7페이지 분량 샘플 데이터 구성
- [x] 변경사항 카드 UI 구성 (분류, 변경 타입, 영향도, 이미지, 요약, 태그)
- [x] 변경사항 대상별 이미지 매핑 (챔피언, 시너지, 아이템, 증강체, 시스템)
- [x] 변경 전/후 상세 비교 영역 접기/펼치기 적용
- [x] 필터/검색/패치 버전 변경 시 페이지와 펼침 상태 초기화
- [x] 검색 결과 없음 상태 UI 구성
- [x] 우측 이번 패치 핵심 요약 패널 구성
- [x] 이전 패치 히스토리 목록 UI 구성
- [x] 히스토리 항목 클릭 시 선택 패치 기준으로 요약/목록/카운트 갱신
- [x] 히스토리 항목 active 상태와 대표 이미지 표시
- [x] 반응형 레이아웃 보완 (필터, 카테고리 탭, 상세 비교 영역)
- [ ] 패치노트 API 응답 타입 정의
  - [ ] 패치 버전 목록 타입
  - [ ] 선택 패치 요약 타입
  - [ ] 변경사항 목록 타입
  - [ ] 변경사항 상세 비교 타입
  - [ ] 이미지 URL / fallback 정책 타입
- [ ] `api/patchNotes.ts` 작성
  - [ ] 패치 히스토리 목록 조회 함수
  - [ ] 선택 패치 요약 조회 함수
  - [ ] 선택 패치 변경사항 목록 조회 함수
  - [ ] 검색어, 카테고리, 변경 타입, 영향도, 페이지 파라미터 전달
- [ ] `hooks/usePatchNotes.ts` 작성
  - [ ] 선택 패치 버전 상태 연결
  - [ ] 필터/검색/페이지 상태 연결
  - [ ] React Query queryKey 분리
  - [ ] staleTime/cacheTime 기준 정의
- [ ] 실제 API 연동 시 샘플 데이터 제거 및 응답 데이터 매핑
- [ ] API 데이터 기준 카테고리별 카운트 계산 연결
- [ ] API 데이터 기준 히스토리 선택 시 상세 목록 재조회 연결
- [ ] Community Dragon CDN 이미지 매핑 보정
  - [ ] champion/item/trait/augment key 기반 이미지 URL 생성
  - [ ] 매핑 실패 항목 fallback 처리
- [ ] 로딩 / 빈 데이터 / 에러 상태 UI 연결
  - [ ] 패치 히스토리 로딩 상태
  - [ ] 패치 변경사항 로딩 상태
  - [ ] 패치 변경사항 빈 결과 상태
  - [ ] API 오류 상태와 재시도 버튼
- [ ] 접근성 점검
  - [ ] 히스토리 선택 버튼 `aria-pressed`
  - [ ] 페이지네이션 `aria-current`
  - [ ] 상세 보기 `aria-expanded`
  - [ ] 키보드 focus-visible 상태
- [ ] QA 체크
  - [ ] 패치 히스토리 클릭 시 버전/요약/목록이 함께 바뀌는지 확인
  - [ ] 이미지 로딩 실패 시 fallback 이미지가 표시되는지 확인
  - [ ] 필터 변경 시 1페이지로 초기화되는지 확인
  - [ ] 검색 결과가 없을 때 빈 상태가 표시되는지 확인
  - [ ] 5개 단위 페이지네이션과 더보기 이동이 동작하는지 확인
- [ ] API 연동

---

## Phase 3 — 마무리

### PR 13: 공통 마무리
- [ ] 전체 페이지 반응형 점검
- [ ] 에러/빈 상태 누락 항목 보완
- [ ] checklist.md 전체 항목 최종 확인
