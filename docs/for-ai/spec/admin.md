<spec domain="admin">

<purpose>
TFTgogo 콘텐츠 관리를 위한 관리자 전용 페이지.
일반 사용자 Layout(사이드바, 헤더)과 완전히 분리된 독립 레이아웃을 사용한다.
Page: /admin/* (AdminLayout 적용)
</purpose>

<routes>
- /admin                → 로그인 (토큰 미보유 시 리다이렉트 대상)
- /admin/decks          → 메타덱 관리
- /admin/hero-augments  → 영웅증강 덱 관리
- /admin/guides         → 게임가이드 관리
- /admin/patch-notes    → 패치노트 관리
- /admin/members        → 회원 관리 (준비 중)
- /admin/community      → 커뮤니티 관리 (준비 중)
</routes>

<layout>
- /admin/* 경로는 일반 Layout을 사용하지 않는다. AdminLayout을 별도 적용한다.
- AdminLayout = AdminSidebar(좌측 고정) + 콘텐츠 영역(우측)
- AdminSidebar 메뉴: 메타덱 관리 / 영웅증강 덱 / 게임가이드 / 패치노트 / 회원 관리 / 커뮤니티 관리 / 로그아웃
- 준비 중인 메뉴(회원, 커뮤니티)는 클릭 시 "준비 중입니다." 화면 표시
</layout>

<frontend-structure>
frontend/src/
├── layouts/AdminLayout.tsx
├── components/admin/AdminSidebar.tsx
└── pages/Admin/
    ├── AdminLogin.tsx
    ├── AdminDecks.tsx
    ├── AdminHeroAugments.tsx
    ├── AdminGuides.tsx
    ├── AdminPatchNotes.tsx
    ├── AdminMembers.tsx       ← 준비 중 화면
    ├── AdminCommunity.tsx     ← 준비 중 화면
    └── Admin.module.css
</frontend-structure>

<api>
<backend>
- GET    /api/admin/decks                         → 덱 목록 조회 (rankFilter 쿼리)
- PATCH  /api/admin/decks/{deckId}                → 덱 큐레이션 저장
- DELETE /api/admin/decks/{deckId}/curation       → 덱 큐레이션 초기화

- GET    /api/admin/hero-augment-decks            → 영웅증강 덱 목록
- POST   /api/admin/hero-augment-decks            → 영웅증강 덱 생성
- PUT    /api/admin/hero-augment-decks/{id}       → 영웅증강 덱 수정
- DELETE /api/admin/hero-augment-decks/{id}       → 영웅증강 덱 삭제

- GET    /api/admin/guides                        → 가이드 목록
- POST   /api/admin/guides                        → 가이드 생성
- POST   /api/admin/guides/import/cdragon         → CDragon에서 챔피언/특성 가이드 항목 가져오기
- PATCH  /api/admin/guides/{guideId}              → 가이드 수정
- DELETE /api/admin/guides/{guideId}              → 가이드 소프트삭제

- GET    /api/admin/patch-notes                   → 패치노트 목록
- POST   /api/admin/patch-notes                   → 패치노트 생성
- PATCH  /api/admin/patch-notes/{patchNoteId}     → 패치노트 수정
- DELETE /api/admin/patch-notes/{patchNoteId}     → 패치노트 소프트삭제
- POST   /api/admin/patch-note-changes            → 패치 변경사항 생성
- PATCH  /api/admin/patch-note-changes/{changeId} → 패치 변경사항 수정
- DELETE /api/admin/patch-note-changes/{changeId} → 패치 변경사항 소프트삭제
</backend>
<frontend>
- frontend/src/api/adminApi.ts
</frontend>
</api>

<auth>
- 인증: X-Admin-Token 요청 헤더
- 토큰은 localStorage(tftgogo_admin_token)에 저장
- 토큰 미보유 또는 401 응답 시 /admin 로그인 화면으로 리다이렉트
- 로그아웃: localStorage 토큰 제거 후 /admin 이동
- AdminTokenFilter가 /api/admin/** 전체를 보호
</auth>

<business-rules>
- Admin API는 큐레이션 도구다. 공개 도메인 검증 규칙을 우회하지 않는다.
- 삭제는 소프트삭제 우선 (deletedAt 필드가 있는 경우)
- JSON 필드(dataJson, highlightsJson 등)는 유효한 JSON이어야 한다
- isCurrent=true인 패치노트는 전체에서 하나만 존재해야 한다
- Swagger 어노테이션은 XxxControllerDocs 인터페이스에 작성, Controller에 직접 작성 금지
- 준비 중인 기능(회원, 커뮤니티)은 "준비 중입니다." 화면만 표시하고 API 호출하지 않는다
</business-rules>

<deck-curation>
- 덱 이름 커스텀, 숨김 처리, 정렬 우선순위 설정 가능
- 챔피언별 헥스 배치판(boardPositions), 운영방법(playGuide), 영웅증강(heroAugments) 편집 가능
- 큐레이션 데이터 키: deck signature (상위 2개 특성 조합)
- 배치판이 설정된 경우 덱 상세에서 CDragon 자동 배치 대신 사용
</deck-curation>

<guide-curation>
- guideType + targetKey + patchVersion으로 가이드 항목 식별
- 관리자는 CDragon 챔피언/특성 가이드 항목을 동일한 guides 테이블로 가져올 수 있다.
- Import는 동일한 guideType + targetKey + patchVersion을 가진 미삭제 행의 콘텐츠를 수정하되 기존 active 상태는 유지하고, 없는 행은 active=true로 새로 생성한다.
- 소프트삭제된 행도 guideType + targetKey + patchVersion을 점유한다.
- dataJson은 유효한 JSON 객체여야 함
- active=false이면 일반 사용자에게 노출 안 됨
</guide-curation>

<patch-note-curation>
- isCurrent는 활성 패치노트 중 하나만 true 가능
- highlightsJson, tagsJson은 JSON 문자열 배열
- 패치 변경사항(PatchChange)은 패치노트에 종속
</patch-note-curation>

<hero-augment-deck>
- 영웅증강 덱은 MetaDeck과 독립적인 별도 엔티티
- grade: S / A / B / C / D
- recommended=true인 덱만 공개 API에서 노출
- champions, heroAugments 필드는 JSON string으로 저장
</hero-augment-deck>

</spec>
