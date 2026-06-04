# docs/for-ai/

Claude/Codex가 작업 시 참조하는 문서 폴더.
**세션 시작 시 전부 로드하지 않고, 해당 작업에서만 호출한다.**

| 파일 | 호출 타이밍 |
|------|------------|
| `review.md` | PR 리뷰 작성 시 |
| `spec-frontend.md` | 프론트엔드 코드 작성/수정 시 |
| `spec-backend.md` | 백엔드 코드 작성/수정 시 |
| `spec-ai.md` | AI 서버 코드 작성/수정 시 |
| `spec/_common.md` | 기능 스펙 파일과 항상 함께 로드 |
| `spec/dashboard.md` | Summoner/Match 작업 시 |
| `spec/decks.md` | Deck 작업 시 |
| `spec/guide.md` | Guide 작업 시 |
| `spec/patch-notes.md` | PatchNotes 작업 시 |
| `spec/ai-recommend.md` | AI Recommend 작업 시 |
| `spec/admin.md` | Admin 작업 시 |
| `coderabbit-fix-prompt.md` | CodeRabbit 수정 작업 시 |
| `coderabbit-decision-workflow.md` | CodeRabbit 판단 흐름 확인 시 |

> `spec/` 하위 파일은 이슈 #197 작업으로 순차 생성 예정.
