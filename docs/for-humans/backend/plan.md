# plan.md - 백엔드 기술 설계

> 백엔드를 어떤 구조로 구현할지 정의한다.
> 특정 담당자 기능의 상세 계약보다 팀 전체 도메인 경계와 공통 설계를 우선한다.

---

## 문서 기준

- 기준 브랜치: `develop`
- 기준 시점: 2026-06-01
- 대상 모듈: `backend/`
- 목적: 팀 전체 백엔드 구현 기준 정리

---

## 전체 모듈 구조

```text
backend/
  build.gradle
  spec-kit/
  src/main/java/com/tftgogo/
    TftgogoApplication.java
    domain/
      member/
      match/
      deck/
      guide/
      patchnote/
      community/
      ai/
    global/
      config/
      exception/
      filter/
      response/
      riot/
  src/main/resources/
    application.yml.example
    log4j2.xml
```

`ai` 도메인은 AI 서버를 직접 포함한다는 의미가 아니라 Spring 백엔드에서 AI 추천 요청/응답을 중계하거나 메타 데이터를 전달하는 어댑터 영역으로 사용한다.

---

## 도메인 역할

| 도메인 | 역할 | 설계 수준 |
| --- | --- | --- |
| `member` | 회원가입, 로그인, 인증/권한, 사용자 식별 | 인증 API와 권한 모델 |
| `match` | 소환사 검색, 전적 조회, 매치 상세 가공 | Riot API 연동 핵심 |
| `deck` | 메타 덱 집계, 덱 목록/상세, 관리자 큐레이션 | 현재 구현 중심 |
| `guide` | 게임가이드 공개 데이터, 관리자 큐레이션 가능 데이터 | 공개 API/로컬 DB smoke 완료, 관리자 API 예정 |
| `patchnote` | 패치노트 목록, 변경사항, 관리자 큐레이션 가능 데이터 | 공개 API/로컬 DB smoke 완료, 관리자 API 예정 |
| `community` | 파티 모집, 게시글, 채팅 연동 | 추후 구현 |
| `ai` | AI 추천 요청 중계, AI 결과 표준화 | ai-server 연동 |
| `global` | 공통 설정, 응답, 예외, 보안, Riot Client | 전 도메인 공통 |

---

## 도메인 패키지 표준

모든 도메인은 가능한 한 같은 패키지 구조를 사용한다.

```text
domain/<domain>/
  controller/
    docs/
  dto/
    request/
    response/
  entity/
  repository/
  service/
    impl/
```

도메인 특성상 Entity 또는 Repository가 필요 없는 경우에는 생략할 수 있다. 예를 들어 외부 서버 중계만 담당하는 `ai` 도메인은 Entity 없이 Controller, DTO, Service만 둘 수 있다.

---

## 공통 패키지 역할

| 패키지 | 역할 |
| --- | --- |
| `global/config` | Security, CORS, Bean, 비동기/스케줄러 설정 |
| `global/exception` | `BusinessException`, `ErrorCode`, `GlobalExceptionHandler` |
| `global/filter` | 관리자 토큰 필터, 인증 필터 |
| `global/response` | `ApiResponse` 공통 응답 |
| `global/riot` | Riot API Client, Riot DTO, TFT 정적 데이터 유틸 |

---

## API 라우팅 원칙

- 모든 Spring API는 `/api` prefix 아래에 둔다.
- 공개 조회 API는 인증 없이 접근할 수 있어야 한다.
- 관리자 API는 `/api/admin/**` 아래에 둔다.
- 현재 인증/회원 API는 기존 구현과 맞춰 `/api/v1/auth/**`를 사용한다.
- 신규 도메인 API는 우선 `/api/{domain}` 형태를 사용하고, 큰 breaking change가 생길 때 버전 prefix 도입을 검토한다.
- 외부 API 응답을 그대로 프론트에 전달하지 않고 백엔드 응답 DTO로 표준화한다.

---

## 주요 API 큰틀

| Method | Path | 담당 도메인 | 접근 | 목적 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/health` 또는 `/health` | `global` | 공개 | 서버 상태 확인 |
| `POST` | `/api/v1/auth/signup` | `member` | 공개 | 회원가입 |
| `POST` | `/api/v1/auth/login` | `member` | 공개 | 로그인 |
| `GET` | `/api/summoners/{gameName}/{tagLine}` | `match` | 공개 | 소환사 기본/랭크 정보 조회 |
| `GET` | `/api/summoners/{gameName}/{tagLine}/matches` | `match` | 공개 | 소환사 전적 목록 조회 |
| `POST` | `/api/summoners/{gameName}/{tagLine}/refresh` | `match` | 인증 또는 제한 필요 | 전적 최신화 |
| `GET` | `/api/decks/meta` | `deck` | 공개 | 메타 덱 목록 조회 |
| `GET` | `/api/decks/{deckId}` | `deck` | 공개 | 메타 덱 상세 조회 |
| `POST` | `/api/decks/meta/aggregate` | `deck` | 관리자 | 메타 덱 집계 실행 |
| `GET` | `/api/guide` | `guide` | 공개 | 게임가이드 카탈로그 조회 |
| `GET` | `/api/guide/{tab}` | `guide` | 공개 | 게임가이드 탭별 조회 |
| `GET` | `/api/patch-notes` | `patchnote` | 공개 | 패치노트 목록 조회 |
| `GET` | `/api/patch-notes/{version}/changes` | `patchnote` | 공개 | 패치 변경사항 조회 |
| `GET` | `/api/community/posts` | `community` | 공개 | 파티 모집 글 목록 조회 |
| `POST` | `/api/community/posts` | `community` | 인증 필요 | 파티 모집 글 작성 |
| `POST` | `/api/ai/recommend` | `ai` | 공개 또는 제한 필요 | AI 추천 요청 |

위 경로는 팀 전체 기준 큰틀이다. 실제 구현 시 프론트 호출부와 최종 합의 후 세부 query parameter와 DTO를 확정한다.

---

## 관리자 API 큰틀

| Method | Path | 담당 도메인 | 목적 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/decks` | `deck` | 관리자용 덱 목록 조회 |
| `PATCH` | `/api/admin/decks/{deckId}` | `deck` | 덱 큐레이션 수정 |
| `DELETE` | `/api/admin/decks/{deckId}/curation` | `deck` | 덱 큐레이션 초기화 |
| `GET` | `/api/admin/guides` | `guide` | 관리자용 가이드 목록 조회 |
| `POST` | `/api/admin/guides` | `guide` | 가이드 항목 생성 |
| `PATCH` | `/api/admin/guides/{guideId}` | `guide` | 가이드 항목 수정 |
| `DELETE` | `/api/admin/guides/{guideId}` | `guide` | 가이드 항목 숨김/삭제 |
| `GET` | `/api/admin/patch-notes` | `patchnote` | 관리자용 패치노트 목록 조회 |
| `POST` | `/api/admin/patch-notes` | `patchnote` | 패치노트 생성 |
| `PATCH` | `/api/admin/patch-notes/{patchNoteId}` | `patchnote` | 패치노트 수정 |
| `DELETE` | `/api/admin/patch-notes/{patchNoteId}` | `patchnote` | 패치노트 숨김/삭제 |
| `POST` | `/api/admin/patch-note-changes` | `patchnote` | 패치 변경사항 생성 |
| `PATCH` | `/api/admin/patch-note-changes/{changeId}` | `patchnote` | 패치 변경사항 수정 |
| `DELETE` | `/api/admin/patch-note-changes/{changeId}` | `patchnote` | 패치 변경사항 숨김/삭제 |
| `GET` | `/api/admin/community/posts` | `community` | 관리자용 모집글 목록 조회 |
| `PATCH` | `/api/admin/community/posts/{postId}` | `community` | 모집글 상태 관리 |

관리자 API는 `X-Admin-Token`을 우선 사용하고, 추후 회원/권한 체계가 안정화되면 `ROLE_ADMIN` 기반으로 전환한다.

---

## SecurityConfig 설계

공개 접근 허용 후보:

```text
/health
/api/health
/api/v1/auth/login
/api/v1/auth/signup
GET /api/summoners/**
GET /api/decks/meta
GET /api/decks/{deckId}
GET /api/guide/**
GET /api/patch-notes/**
GET /api/community/posts/**
/swagger-ui/**
/v3/api-docs/**
```

보호 대상:

```text
/api/admin/**
/api/member/**
POST /api/decks/meta/aggregate
POST /api/summoners/{gameName}/{tagLine}/refresh
POST /api/community/posts
쓰기 비용 또는 외부 API 비용이 큰 AI 추천 API
쓰기 권한이 필요한 community API
```

현재 단계에서는 관리자 API를 `AdminTokenFilter`가 보호한다. 로그인 기반 인증이 들어오면 Security chain을 다시 정리한다.

---

## 데이터 흐름

### Riot API 기반 조회

1. 프론트가 소환사 또는 메타 덱 데이터를 요청한다.
2. 백엔드는 Riot API 또는 저장된 내부 데이터를 조회한다.
3. 백엔드는 Community Dragon 식별자를 활용해 이름, 이미지, 특성 정보를 보강한다.
4. 백엔드는 프론트 전용 Response DTO로 가공해 반환한다.

### 내부 집계 데이터

1. 스케줄러 또는 관리자 트리거가 집계를 시작한다.
2. Riot Match 데이터를 수집한다.
3. 백엔드는 덱 시그니처, 유닛, 아이템, 통계 데이터를 계산한다.
4. 기존 패치/랭크 조건의 데이터를 교체하거나 갱신한다.
5. 공개 API는 관리자 큐레이션 레이어를 반영해 반환한다.

### 관리자 큐레이션 데이터

1. 자동 수집/집계 데이터의 품질 한계를 관리자가 보완한다.
2. 공개 여부, 표시 이름, 정렬 순서, 관리자 메모 등을 별도 레이어로 저장한다.
3. 공개 API는 자동 데이터와 큐레이션 데이터를 결합해 반환한다.
4. Guide/PatchNotes처럼 자동 집계보다 큐레이션이 우선인 콘텐츠는 도메인 테이블에 active row로 저장하고, 관리자 API에서 생성/수정/soft delete한다.
5. 여러 패치 버전이 쌓이는 콘텐츠 API는 기본 조회에서 서로 다른 패치 데이터를 섞지 않는다.

### AI 추천 연동

1. 프론트가 현재 보유 유닛/아이템 정보를 백엔드로 전달한다.
2. 백엔드는 요청을 검증하고 ai-server에 전달한다.
3. 백엔드는 ai-server 응답을 서비스 공통 응답 포맷으로 표준화한다.
4. AI 서버 장애 시 프론트가 이해할 수 있는 오류 응답을 반환한다.

---

## DB 설계 방향

- `member`: 사용자, 인증 정보, 권한.
- `match`: 소환사, 매치, 참가자, 유닛, 아이템, 시너지.
- `deck`: 메타 덱, 덱 유닛, 덱 특성, 추천 아이템, 관리자 큐레이션.
- `guide`: 가이드 항목, 가이드 유형, 패치 버전, 공개 여부, JSON 표시 데이터.
- `patchnote`: 패치노트, 변경사항, 변경 카테고리, 현재 패치 여부, 공개 여부.
- `community`: 모집글, 댓글 또는 채팅 연계 식별자, 모집 상태.

세부 컬럼은 각 도메인 구현 이슈에서 확정한다. 팀 전체 plan 문서에는 테이블 책임과 관계의 큰 방향만 둔다.

---

## 구현 우선순위 큰틀

1. 공통 응답, 예외, 보안 규칙 정리.
2. Riot API 연동과 전적 조회 기반 확보.
3. 메타 덱 집계와 관리자 큐레이션 안정화.
4. 프론트 주요 화면이 의존하는 공개 API 계약 확정.
5. 게임가이드 콘텐츠 도메인의 공개 API와 로컬 DB smoke 완료 후 관리자 API로 큐레이션 입력 흐름을 연결한다.
6. 패치노트 콘텐츠 도메인의 공개 API와 로컬 DB smoke 완료 후 관리자 API로 큐레이션 입력 흐름을 연결한다.
7. Community 같은 게시성 도메인의 관리자 연동 준비.
8. AI 추천 연동 안정화.
9. 테스트, Swagger, 운영 로그 정리.
