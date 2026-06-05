# Codex CodeRabbit Fix Prompt

아래 프롬프트는 CodeRabbit 리뷰 코멘트를 Codex로 수정할 때 사용한다. Claude Code의 기존 프롬프트를 Codex와 이 저장소 컨벤션에 맞게 보정한 버전이다.

## 기본 실행 프롬프트

```text
@coderabbit 현재 브랜치 또는 PR의 리뷰 코멘트를 확인해줘.

TFT-gogo 프로젝트 컨벤션과 사용자가 남긴 판단 코멘트를 기준으로 CodeRabbit 코멘트를 분류하고 필요한 수정만 적용해줘.

작업 원칙:
- 답변과 수정 요약은 한국어로 작성한다.
- 사용자가 남긴 판단값을 최우선으로 따른다.
- 판단값은 fix / skip / discuss로 해석한다.
- fix로 표시된 항목만 수정한다.
- skip 또는 discuss 항목은 임의로 수정하지 말고 보류 이유를 요약한다.
- Critical / Warning / Suggestion으로 심각도를 구분한다.
- Critical, 빌드 실패, 런타임 오류, 보안 위험, 사용자 흐름 깨짐은 우선 수정한다.
- 컨벤션 위반은 기능이 동작하더라도 지적하고 가능한 범위에서 수정한다.
- 스타일 선호 수준의 Suggestion은 무조건 반영하지 말고 적용/보류 이유를 남긴다.
- 현재 diff와 관련 없는 리팩터링은 하지 않는다.
- 기존 파일 구조, 네이밍, API 패턴, CSS Modules 패턴을 따른다.

Frontend 기준:
- React + Vite + TypeScript 기준으로 작성한다.
- 컴포넌트는 PascalCase.tsx, 훅/유틸/API는 camelCase.ts를 사용한다.
- CSS Modules만 사용하고 Tailwind, styled-components, inline style은 사용하지 않는다.
- 색상/크기/radius는 variables.css의 CSS 변수를 우선 사용한다.
- any는 금지하고 unknown 또는 명시적 타입을 사용한다.
- interface를 우선 사용하고 유니온/인터섹션에는 type을 사용한다.
- 서버 상태는 TanStack Query, 전역 상태는 Zustand를 사용한다.
- HTTP 요청은 api/ 함수와 axiosInstance를 통해서만 처리한다.
- 컴포넌트 내부에서 fetch 또는 직접 axios import를 사용하지 않는다.
- Community Dragon 이미지 URL은 communityDragonAssets.ts 헬퍼를 사용한다.

Backend 기준:
- 도메인별 Exception 클래스를 만들지 말고 BusinessException(ErrorCode.XXX)을 사용한다.
- API 응답은 ResponseEntity<ApiResponse<T>>로 통일한다.
- null 반환 대신 Optional + orElseThrow()를 사용한다.
- System.out.println은 금지하고 Log4j2 Logger를 사용한다.
- 비밀번호, JWT, 인증번호는 로그에 남기지 않는다.
- Random 대신 SecureRandom, @Value 직접 주입 대신 @ConfigurationProperties를 사용한다.
- Swagger 어노테이션은 XxxControllerDocs 인터페이스에 작성한다.

FastAPI 기준:
- api/는 라우터, services/는 비즈니스 로직, models/는 Pydantic 스키마, core/는 설정과 의존성을 담당한다.
- 응답은 BaseResponse 계열로 통일한다.
- print() 대신 logging을 사용한다.

수정 후:
- 변경 파일을 요약한다.
- 고친 CodeRabbit 코멘트와 보류한 코멘트를 구분해 설명한다.
- 가능한 검증 명령을 실행하고 결과를 요약한다.
- 검증을 실행하지 못했다면 이유를 명확히 말한다.
- PR이 이미 있으면 같은 브랜치에 커밋을 푸시해서 기존 PR을 업데이트한다.
- PR이 없으면 codex/coderabbit-fix-<작업명> 브랜치를 만들고 develop 대상으로 PR을 생성한다.
```

## 사용자 판단 코멘트 형식

PR에서 CodeRabbit 리뷰를 확인한 뒤 사용자는 아래 형식으로 코멘트를 남긴다.

```md
/codex fix-coderabbit

판단:
- fix: CR-1, CR-3
- skip: CR-2 - 스타일 제안이라 이번 PR 범위에서 제외
- discuss: CR-4 - 팀 컨벤션과 충돌해서 확인 필요

요청:
- fix 항목만 수정
- 수정 후 검증 실행
- 기존 PR 브랜치에 커밋 push
```

CodeRabbit 코멘트에 명확한 번호가 없으면 파일 경로와 코멘트 요지를 함께 쓴다.

```md
/codex fix-coderabbit

판단:
- fix: frontend/src/pages/Decks/Decks.tsx - any 타입 제거
- fix: frontend/src/api/deckApi.ts - axiosInstance 에러 처리 보강
- skip: frontend/src/pages/Decks/Decks.module.css - 색상 톤 제안은 이번 PR 제외
```

## 짧은 호출용 프롬프트

```text
@coderabbit 현재 PR 리뷰 코멘트와 사용자가 남긴 /codex fix-coderabbit 판단 코멘트를 확인해줘. fix 항목만 TFT-gogo 컨벤션 기준으로 수정하고, skip/discuss 항목은 보류해. 수정 후 검증하고 기존 PR 브랜치에 커밋 push하거나 PR이 없으면 새 PR을 만들어줘.
```

## Codex 단독 호출용 프롬프트

CodeRabbit 플러그인 멘션 없이 Codex에게 직접 맡길 때는 아래처럼 요청한다.

```text
현재 브랜치의 CodeRabbit 리뷰 코멘트와 사용자가 남긴 /codex fix-coderabbit 판단 코멘트를 기준으로 필요한 수정만 적용해줘.
AGENTS.md의 TFT-gogo 컨벤션을 반드시 따르고, Critical/Warning을 우선 처리해줘.
fix 항목만 수정하고 skip/discuss 항목은 보류 이유를 요약해줘.
수정 후 가능한 검증 명령을 실행하고 결과를 알려줘.
기존 PR이 있으면 같은 브랜치에 커밋 push하고, PR이 없으면 develop 대상으로 새 PR을 만들어줘.
```

## 검증 명령 가이드

- Frontend 변경: `cd frontend` 후 `npm run build` 또는 프로젝트에 있는 타입체크/린트 스크립트
- Backend 변경: `cd backend` 후 Gradle 테스트 또는 해당 Service 테스트
- FastAPI 변경: `cd ai-server` 후 사용 중인 테스트/정적 검사 명령

실행 가능한 명령은 실제 `package.json`, Gradle 설정, Python 설정을 먼저 확인한 뒤 선택한다.
