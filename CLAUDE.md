# CLAUDE.md — TFT-gogo AI 코드리뷰 가이드

> Claude가 이 프로젝트의 PR을 리뷰할 때 참조하는 파일이다.
> 컨벤션 위반, 구조 문제, 품질 이슈를 시니어 엔지니어 관점에서 리뷰한다.

---

## 프로젝트 개요

TFT(팀파이트 택티스) 전적 검색 및 메타 가이드 서비스.
모노레포 구조: `frontend/` (React) + `backend/` (Spring Boot) + `ai-server/` (FastAPI)

---

## 리뷰 원칙

1. **컨벤션 위반을 최우선으로 지적한다** — 팀 규칙을 어긴 코드는 기능이 맞아도 반드시 언급
2. **심각도를 구분한다** — `🚨 Critical` / `⚠️ Warning` / `💡 Suggestion`
3. **이유를 설명한다** — 왜 문제인지, 어떻게 고쳐야 하는지 구체적으로 작성
4. **잘한 점도 언급한다** — 긍정적인 피드백 포함
5. **한국어로 리뷰한다**

---

## 리뷰 출력 형식

```
## 🤖 Claude AI 코드리뷰

### 📋 변경사항 요약
(PR에서 무엇을 변경했는지 1~3줄 요약)

### 🚨 Critical (반드시 수정)
- ...

### ⚠️ Warning (수정 권장)
- ...

### 💡 Suggestion (선택적 개선)
- ...

### ✅ 잘된 점
- ...
```

---

## Frontend 컨벤션 체크리스트

### 스타일
- [ ] CSS Modules 사용 (`*.module.css`) — **Tailwind 사용 절대 금지**
- [ ] 색상/크기 하드코딩 금지 → `variables.css`의 CSS 변수 사용
  - 새로운 색상/간격이 필요하면 `variables.css`에 토큰 추가 후 참조
  - 기존 토큰 목록: `--color-*`, `--tone-*`, `--space-*`, `--font-size-*`, `--radius-*`
- [ ] CSS Modules 클래스명은 **camelCase** — snake_case(`tone_gold`) 금지 → `toneGold`
- [ ] 동적 클래스 매핑은 `Record<string, string>` 상수 Map으로 관리 (bracket notation 직접 사용 지양)
  ```tsx
  // ❌ 금지
  styles[`tone_${t.tone}`]
  // ✅ 권장
  const TONE_CLASS_MAP: Record<string, string> = { gold: styles.toneGold, ... }
  TONE_CLASS_MAP[t.tone]
  ```
- [ ] 컴포넌트 내부에 이미지 URL 하드코딩 금지 → `communityDragonAssets.ts` 사용

### TypeScript
- [ ] `any` 사용 금지 → `unknown` 또는 명시적 타입 사용
- [ ] 타입 정의는 `interface` 우선, 유니온/인터섹션만 `type` 사용
- [ ] 함수형 컴포넌트만 사용 (클래스 컴포넌트 금지)
- [ ] 모든 `async` 함수는 `try/catch` 처리 필수

### 파일/폴더 구조
- [ ] 컴포넌트 파일명: `PascalCase.tsx`
- [ ] 훅/유틸 파일명: `camelCase.ts`
- [ ] 컴포넌트는 `export default` 사용
- [ ] 페이지 전용 컴포넌트 → `pages/<PageName>/components/` 위치
- [ ] 2개 이상 페이지 공용 컴포넌트 → `components/common/` 이동

### React 패턴
- [ ] 서버 상태 관리: TanStack Query 사용 (직접 useState로 fetch 금지)
- [ ] 전역 상태 관리: Zustand 사용
- [ ] HTTP 요청: axiosInstance 사용 (직접 fetch/axios import 금지)

---

## Backend 컨벤션 체크리스트

### 예외 처리
- [ ] 도메인별 Exception 클래스 생성 금지
- [ ] `BusinessException(ErrorCode.XXX)` 패턴 사용
- [ ] `GlobalExceptionHandler`에서 모든 예외 처리

### 응답 표준화
- [ ] 모든 API 응답은 `ApiResponse` 클래스로 통일
- [ ] 성공: `ApiResponse.success("메시지", data)`
- [ ] 실패: `ApiResponse.fail("메시지", HttpStatus)`
- [ ] 반환 타입: `ResponseEntity<ApiResponse<T>>`

### 코드 품질
- [ ] `null` 반환 금지 → `Optional + orElseThrow()` 사용
- [ ] `System.out.println` 금지 → Log4j2 Logger 사용
- [ ] `java.util.Random` 금지 → `SecureRandom` 사용
- [ ] `@Value` 지양 → `@ConfigurationProperties` 사용
- [ ] Java stream 필터링 금지 → DB 레벨에서 처리

### 보안
- [ ] 비밀번호/인증번호 평문 저장 금지 → BCrypt 암호화
- [ ] JWT 토큰 로그 출력 금지

### 계층 구조
- [ ] 서비스 구현체는 `service/impl/` 위치
- [ ] Swagger 어노테이션은 `XxxControllerDocs` 인터페이스에만 작성

---

## PR 규칙 체크리스트

- [ ] PR 제목 형식 준수: `feat: ...` / `fix: ...` / `refactor: ...` / `docs: ...` / `chore: ...`
- [ ] 1 PR = 1 기능 단위 (파일 수 20개 이하 권고)
- [ ] 브랜치명 형식: `feature/기능명`, `fix/버그명`, `chore/작업명`
- [ ] `main` 브랜치 직접 push 금지

---

## 로깅 규칙

```java
// 선언
private static final Logger logger = LogManager.getLogger(Xxx.class);

// 레벨 구분
logger.info("정상 흐름");   // 성공 케이스
logger.warn("비정상이지만 시스템 동작");  // 실패, 잘못된 요청
logger.error("시스템 오류", e);  // 예외 발생
```

**절대 로그에 남기면 안 되는 것**: 비밀번호, JWT 토큰, 인증번호

---

## 테스트 규칙

- Service 레이어만 단위 테스트
- `given/when/then` 패턴 사용
- `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- 실제 DB/외부 서비스 연결 없이 Mock으로 테스트
