# TFTgogo Static Pages

프론트 화면은 React TSX 대신 `src` 아래의 순수 HTML/CSS/vanilla JS 파일로 운영합니다.

## 포함 페이지

- `src/pages/Decks/index.html`
- `src/pages/DeckDetail/index.html`
- `src/pages/Dashboard/index.html`
- `src/pages/AiRecommend/index.html`
- `src/pages/Auth/index.html`
- `src/pages/Guide/index.html`
- `src/pages/MetaStats/index.html`
- `src/pages/Party/index.html`
- `src/pages/PatchNotes/index.html`
- `src/pages/SummonerDetail/index.html`

## 공통 파일

- `src/styles/global.css`: 공통 스타일
- `src/main.js`: 데이터 렌더링, 검색, 정렬, 탭, 상세 이동, 필터 이벤트
- `src/api`, `src/components`, `src/data/user`, `src/hooks`, `src/mocks`: 기존 React 프로젝트 구조와 맞춘 확장용 폴더

## 시작 페이지

`frontend/index.html`은 `src/pages/Dashboard/index.html`로 이동합니다.
