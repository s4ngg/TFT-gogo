# CLAUDE.md — TFT-gogo AI 코드리뷰 가이드

TFT(팀파이트 택티스) 전적 검색 및 메타 가이드 서비스.
모노레포 구조: `frontend/` (React) + `backend/` (Spring Boot) + `ai-server/` (FastAPI)

---

## 리뷰 원칙

1. 컨벤션 위반을 최우선으로 지적한다 — 기능이 맞아도 반드시 언급
2. 심각도를 구분한다 — `🚨 Critical` / `⚠️ Warning` / `💡 Suggestion`
3. 이유를 설명한다 — 왜 문제인지, 어떻게 고쳐야 하는지 구체적으로 작성
4. 잘한 점도 언급한다
5. 한국어로 리뷰한다

---

## 리뷰 시 참조 파일

| PR 영역 | 컨벤션 파일 |
|---------|-----------|
| 리뷰 출력 형식 + PR 규칙 | `docs/for-ai/review.md` |
| 프론트엔드 체크리스트 | `docs/for-ai/frontend/conventions.md` |
| 백엔드 체크리스트 | `docs/for-ai/backend/conventions.md` |
| AI 서버 체크리스트 | `docs/for-ai/ai-server/conventions.md` |

---

## 실패 계약 변경 시 필수 확인

예외 처리, fallback, null 반환, 인증 실패, 외부 API 실패 처리 로직을 수정할 때는 변경 파일만 보지 말고 함수 계약을 기준으로 호출부와 테스트를 함께 갱신한다.

특히 아래가 바뀌면 반드시 관련 테스트를 검색해 수정한다:

- `return null`이 `throw BusinessException`으로 바뀐 경우
- `catch` 범위가 넓어지거나 좁아진 경우
- 200/null 응답이 4xx/5xx 예외 응답으로 바뀐 경우
- fallback/mock 동작이 제거되거나 실제 에러로 전파되는 경우
- 인증 실패와 일반 실패를 구분하도록 바뀐 경우

수정 전후로 다음을 표로 정리한다:

- 정상 경로
- 데이터 없음 경로
- 외부 API 실패 경로
- 인증/권한 실패 경로
- 예상치 못한 예외 경로

그리고 다음 키워드로 기존 테스트를 검색해 계약이 맞는지 확인한다:

`null`, `fallback`, `실패`, `예외`, `BusinessException`, `인증`, `401`, `502`, `AI_SERVER`, `NOT_FOUND`

기능 코드의 실패 계약이 바뀌면 테스트명과 기대값도 반드시 함께 변경한다.
