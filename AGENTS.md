# TFT-gogo Codex Agent Instructions

이 저장소에서 작업하는 Codex 에이전트는 아래 규칙을 우선 적용한다. 답변, 리뷰, 수정 요약은 한국어로 작성한다.

## 프로젝트 맥락

- TFT-gogo는 TFT 전적 검색 및 메타 가이드 서비스다.
- 모노레포 구조는 `frontend/` React + Vite + TypeScript, `backend/` Spring Boot, `ai-server/` FastAPI로 구성된다.
- 기존 코드 스타일과 폴더 구조를 먼저 확인하고, 변경 범위는 현재 요청과 CodeRabbit 코멘트에 필요한 부분으로 제한한다.

## CodeRabbit 코멘트 수정 원칙

- CodeRabbit 코멘트는 `Critical`, `Warning`, `Suggestion`으로 나누어 판단한다.
- 사용자가 PR 코멘트나 요청 본문에 남긴 판단표를 최우선 입력으로 사용한다.
- 판단값은 `fix`, `skip`, `discuss`를 사용한다.
- `fix`로 표시된 항목만 수정한다. 단, 보안 위험, 빌드 실패, 명백한 런타임 오류는 사용자에게 위험을 알리고 수정 필요성을 강하게 설명한다.
- `skip` 또는 `discuss`로 표시된 항목은 임의로 수정하지 않고 보류 이유를 요약한다.
- `Critical`과 실제 버그, 빌드 실패, 보안 위험, 런타임 오류, 사용자 흐름 깨짐은 반드시 우선 수정한다.
- `Warning`은 프로젝트 컨벤션 위반, 유지보수성 저하, 테스트 누락 여부를 기준으로 수정한다.
- `Suggestion`은 스타일 선호만 있으면 무조건 반영하지 말고, 적용/보류 이유를 요약한다.
- 컨벤션 위반은 기능이 동작하더라도 리뷰 요약에 명시하고 가능한 범위에서 고친다.
- 수정 후 어떤 코멘트를 고쳤고 어떤 코멘트를 보류했는지 파일 경로와 함께 설명한다.
- PR이 이미 있으면 같은 브랜치에 커밋을 푸시해 기존 PR을 업데이트한다.
- PR이 없으면 `codex/coderabbit-fix-<작업명>` 형식의 브랜치를 만들고 PR을 생성한다.

## Frontend 컨벤션

- `frontend/src`는 TypeScript 기준으로 작성한다. 컴포넌트 파일은 `PascalCase.tsx`, 훅/유틸/API 파일은 `camelCase.ts`를 사용한다.
- 컴포넌트는 함수형 컴포넌트와 `export default`를 사용한다.
- 타입 정의는 `interface`를 우선 사용하고, 유니온/인터섹션 타입에는 `type`을 사용한다.
- `any`는 금지한다. `unknown` 또는 명시적 타입을 사용한다.
- 모든 `async` 함수는 실패 경로를 고려한다. API 계층에서는 `try/catch`, 호출부에서는 TanStack Query의 에러 상태 등 기존 패턴을 따른다.
- 서버 상태는 TanStack Query로 관리한다. `useEffect` + `useState`로 API 데이터를 직접 가져오지 않는다.
- 전역 클라이언트 상태는 Zustand로 관리한다. 서버 데이터는 Zustand에 저장하지 않는다.
- HTTP 요청은 `frontend/src/api/axiosInstance.ts`와 `api/` 폴더의 함수로 처리한다. 컴포넌트에서 `fetch` 또는 직접 `axios` import를 사용하지 않는다.
- 스타일은 CSS Modules만 사용한다. Tailwind, styled-components, inline style은 사용하지 않는다.
- 색상, 크기, radius 등 디자인 토큰은 `frontend/src/styles/variables.css`의 CSS 변수를 우선 사용한다.
- 컴포넌트 내부에 Community Dragon CDN URL을 하드코딩하지 않는다. `frontend/src/api/communityDragonAssets.ts`의 헬퍼를 사용한다.
- 페이지 전용 컴포넌트는 `pages/<PageName>/components/`에 둔다.
- 2개 이상 페이지에서 공유하는 컴포넌트는 `components/common/`으로 이동한다.
- CSS Modules 클래스명은 camelCase를 사용하고, 전역 스타일은 전역 CSS 파일에서만 관리한다.
- Vite 환경변수는 `VITE_` 접두사를 사용하고 `.env` 파일은 커밋하지 않는다.

## Backend 컨벤션

- 도메인별 Exception 클래스를 새로 만들지 않는다.
- 예외는 `BusinessException(ErrorCode.XXX)` 패턴과 `GlobalExceptionHandler`로 통합 처리한다.
- 모든 API 응답은 `ApiResponse`로 통일하고 반환 타입은 `ResponseEntity<ApiResponse<T>>`를 사용한다.
- `null` 반환을 피하고 `Optional` + `orElseThrow()`를 사용한다.
- Request DTO에는 `toEntity()`를 둘 수 있고, Response DTO는 `from()` 또는 `of()` 정적 팩토리 메서드를 사용한다.
- `@NotNull`, `@NotBlank` 같은 검증 어노테이션은 Request DTO에만 사용한다.
- `System.out.println`은 금지하고 Log4j2 Logger를 사용한다.
- 비밀번호, JWT 토큰, 인증번호 등 민감 정보는 로그에 남기지 않는다.
- `java.util.Random`은 금지하고 `SecureRandom`을 사용한다.
- `@Value` 직접 주입은 지양하고 `@ConfigurationProperties`를 사용한다.
- Java stream으로 대량 필터링하기보다 DB 레벨에서 조회 조건을 처리한다.
- 서비스 구현체는 `service/impl/`에 둔다.
- Swagger 어노테이션은 `XxxControllerDocs` 인터페이스에만 작성하고 Controller는 이를 구현한다.
- `@Async void`는 금지하고 `CompletableFuture<Void>`를 반환한다.
- `SimpleAsyncTaskExecutor` 대신 명시적인 `ThreadPoolTaskExecutor`를 설정한다.

## FastAPI 컨벤션

- `api/`는 라우터와 엔드포인트 정의만 담당한다.
- `services/`는 비즈니스 로직과 RAG/추천 로직을 담당한다.
- `models/`는 Pydantic 스키마를 담당한다.
- `core/`는 설정, DB 연결, 의존성 주입을 담당한다.
- 응답은 공통 `BaseResponse` 계열 스키마로 통일한다.
- `print()`는 금지하고 `logging` 모듈을 사용한다.
- RAG 파이프라인은 pgvector, OpenAI Embeddings API, LangChain 사용 원칙을 따른다.

## 테스트와 검증

- Frontend 변경 후 가능한 경우 `frontend`에서 타입체크 또는 빌드를 실행한다.
- Backend 신규/수정 Service 로직에는 Service 레이어 단위 테스트를 우선 작성한다.
- Backend 테스트는 `given/when/then`, `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` 패턴을 사용한다.
- 실제 DB나 외부 서비스에 연결하지 않고 Mock으로 테스트한다.
- 테스트 메서드명은 기존 팀 컨벤션을 따라 한글 이름을 허용한다.

## Git/PR 규칙

- 작업 브랜치는 `develop`에서 분기하는 것을 원칙으로 한다.
- 브랜치명은 `feature/기능명`, `fix/버그명`, `chore/작업명` 형식을 따른다. Codex 전용 작업 브랜치가 필요하면 앱 기본값인 `codex/` 접두사를 사용할 수 있다.
- PR 제목과 커밋 메시지는 `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`, `test: ...`, `chore: ...`, `style: ...` 형식을 사용한다.
- `main`과 `develop`에는 직접 push하지 않는다.
- 1 PR은 1 기능 단위를 권장하고, 파일 수 20개 이하를 목표로 한다.
