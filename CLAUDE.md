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
| 프론트엔드 체크리스트 | `docs/for-ai/spec-frontend.md` |
| 백엔드 체크리스트 | `docs/for-ai/spec-backend.md` |
| AI 서버 체크리스트 | `docs/for-ai/spec-ai.md` |
