# tasks.md - 백엔드 작업 순서 정의

> 팀 전체 백엔드 작업을 어떤 순서로 진행할지 정의한다.
> 상세 구현 이슈는 각 도메인별 이슈에서 별도로 쪼갠다.

---

## Phase 0 - Spec Kit 정리

- [x] 백엔드 `spec-kit` 디렉터리 기준 문서 구조 정리
- [x] `constitution.md` 백엔드 개발 규칙 작성
- [x] `plan.md` 백엔드 기술 구조 작성
- [x] `spec.md` 백엔드 기능 요구사항 작성
- [x] `tasks.md` 백엔드 작업 순서 작성
- [x] `checklist.md` 백엔드 검증 기준 작성

---

## Immediate Backlog - 2026-06-01 기준

### PR 리뷰 후속

- [ ] PR #113: `memberApi.ts`, `AuthPage.module.css` EOF 공백 정리 후 `git diff --check` 재확인
- [ ] PR #124: 덱모음 시너지 태그 CSS의 디자인 토큰 적용과 CSS Modules camelCase 클래스명 정리
- [ ] PR #127: 배치판 아이템 유지, 삭제된 챔피언 편집 상태 초기화, `boardPositions` JSON 검증, 신규 CSS 토큰 적용
- [ ] PR #126: Guide/PatchNotes API 정책 명확화 후 작성자가 아닌 팀원 승인 요청

### 백엔드 선행 작업

- [ ] #129: 공개 메타 덱 수동 집계 API에 관리자 보호 적용
- [ ] #135: 관리자 덱 조회 실패를 `BusinessException(ErrorCode.DECK_NOT_FOUND)` 기반 404로 처리
- [ ] #136: 덱 큐레이션 JSON 요청값 검증 추가
- [ ] #133: 최신 패치 조회 로직을 숫자 버전 기준으로 통일
- [ ] #132: 메타 덱 집계 부분 실패 시 기존 데이터 삭제 방지
- [ ] #137: 메타 덱 signature를 안정적인 canonical key로 생성
- [ ] #130/#131: 메타 덱 집계 작업을 비동기 job으로 분리하고 서버 시작 시 중복 실행 방지
- [ ] #134: 공개 메타 덱 조회 N+1 제거

### Guide/PatchNotes API 연결 계획

- [ ] 프론트 `axiosInstance`의 baseURL이 `/api`이므로 프론트 API 함수는 `/guide`, `/patch-notes`처럼 `/api` prefix 없이 호출한다.
- [ ] 백엔드 공개 Controller는 `/api/guide`, `/api/guide/{tab}`, `/api/patch-notes`, `/api/patch-notes/{version}/changes`를 제공한다.
- [ ] 모든 공개 응답은 `ApiResponse<T>`와 camelCase 필드명을 사용한다.
- [ ] Guide `cost` 필터는 `champions` 탭에서만 적용하고 다른 탭에서는 무시한다.
- [ ] Guide `dataJson`은 공개 응답에서 문자열이 아니라 JSON object로 반환한다.
- [ ] `GET /api/patch-notes`는 lightweight 목록만 반환하고, 변경사항 목록과 통계는 `/changes` endpoint에서 페이지 단위로 조회한다.
- [ ] 관리자 API는 이후 `/api/admin/guides`, `/api/admin/patch-notes`, `/api/admin/patch-note-changes` 아래에 추가한다.

---

## Phase 1 - 공통 기반

- [ ] `ApiResponse` 사용 범위 점검
- [ ] `BusinessException` / `ErrorCode` 사용 범위 점검
- [ ] `GlobalExceptionHandler` 오류 응답 포맷 점검
- [ ] `SecurityConfig` 공개/보호 경로 정리
- [ ] 공개 API의 HTTP Method 기준 허용 범위 정리
- [ ] 관리자 API 보호 방식 점검
- [ ] API 버전 prefix 정책 확정
- [ ] DB 스키마 변경과 마이그레이션 관리 방식 확정
- [ ] CORS 허용 origin과 method 점검
- [ ] Log4j2 사용 규칙 점검
- [ ] Swagger docs 인터페이스 작성 규칙 적용
- [ ] 공통 페이지 응답 DTO 또는 도메인별 페이지 응답 규칙 확정

---

## Phase 2 - Member/Auth

- [ ] 회원가입 API 구현 또는 현재 구현 점검
- [ ] 로그인 API 구현 또는 현재 구현 점검
- [ ] 인증 토큰 발급/검증 흐름 점검
- [ ] 사용자 권한 모델 확정
- [ ] 인증 실패/권한 실패 ErrorCode 정리
- [ ] 민감 정보 로그 노출 여부 점검
- [ ] 인증 관련 테스트 작성

---

## Phase 3 - Summoner/Match

- [ ] Riot Account API 연동
- [ ] Riot Summoner API 연동
- [ ] Riot League API 연동
- [ ] Riot Match API 연동
- [ ] 소환사 검색 응답 DTO 작성
- [ ] 최근 전적 목록 응답 DTO 작성
- [ ] 매치 상세 응답 DTO 작성
- [ ] 게임 유형 변환 정책 적용
- [ ] 스테이지 변환 정책 적용
- [ ] 증강/LP 변동값 미제공 정책 반영
- [ ] 전적 최신화 API 구현
- [ ] 외부 API 실패/Rate limit 처리 테스트 작성

---

## Phase 4 - Deck

- [ ] 메타 덱 집계 기준 점검
- [ ] 패치 버전 추출 정책 점검
- [ ] 랭크 필터별 메타 덱 조회 구현 점검
- [ ] 덱 중복 병합 기준 점검
- [ ] 상점 구매 가능 유닛 필터 점검
- [ ] 덱 티어 계산 기준 점검
- [ ] 덱 상세 레벨별 구성 응답 점검
- [ ] 추천 아이템 응답 점검
- [ ] 관리자 덱 큐레이션 API 점검
- [ ] 메타 덱 Service 테스트 보강

---

## Phase 5 - Guide

- [ ] 게임가이드 공개 API 큰틀 확정
- [ ] 가이드 데이터 관리 방식 확정
- [ ] 패치 버전, 공개 여부, 정렬 순서 정책 확정
- [ ] 관리자 연동 필요 범위 확정
- [ ] 세부 API/DTO 기준은 `spec.md` Guide 섹션을 기준으로 구현 이슈에서 확정

---

## Phase 6 - PatchNotes

- [ ] 패치노트 공개 API 큰틀 확정
- [ ] 패치노트 변경사항 관리 방식 확정
- [ ] 카테고리/변경 타입/영향도 enum 정책 확정
- [ ] 관리자 연동 필요 범위 확정
- [ ] 세부 API/DTO 기준은 `spec.md` PatchNotes 섹션을 기준으로 구현 이슈에서 확정

---

## Phase 7 - Community

- [ ] 파티 모집 글 도메인 모델 확정
- [ ] 모집 글 목록/상세/작성 API 확정
- [ ] 모집 상태 변경 정책 확정
- [ ] 검색/필터/페이지네이션 정책 확정
- [ ] 채팅 기능의 백엔드 경계 확정
- [ ] 신고/숨김/관리자 처리 정책 확정

---

## Phase 8 - AI Recommend

- [ ] ai-server 연동 경로 확정
- [ ] AI 추천 요청 DTO 확정
- [ ] AI 추천 응답 DTO 확정
- [ ] AI 서버 장애/타임아웃 처리 정책 확정
- [ ] 메타 덱 데이터 참조 여부 확정
- [ ] AI 추천 API 테스트 작성

---

## Phase 9 - 통합 검증

- [ ] 프론트 API 호출 경로와 백엔드 경로 대조
- [ ] Swagger UI에서 전체 공개/관리자 API 확인
- [ ] 전체 테스트 실행
- [ ] 빌드 실행
- [ ] 주요 API 수동 호출 검증
- [ ] README 또는 팀 공유 문서에 실행 방법 반영
