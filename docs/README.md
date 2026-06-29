# docs/

TFT-gogo 프로젝트 문서 폴더.

---

## for-humans/

팀원이 읽는 문서 폴더.

| 파일/폴더 | 내용 |
|------|------|
| `constitution.md` | 팀 개발 프로세스, 컨벤션 원문 |
| `spec.md` | 서비스 전체 기능 한눈에 보기 (인덱스) |
| `spec/` | 기능별 상세 스펙 |
| `plan.md` | 개발 계획, 마일스톤 |
| `tasks.md` | 작업 목록, 진행 상황 |
| `checklist.md` | 배포, 작업 체크리스트 |
| `screenshots/` | UI 스크린샷 |
| `frontend/` | 프론트엔드 전용 문서 |
| `backend/` | 백엔드 전용 문서 |

> **AI 도구는 이 폴더를 직접 열지 않는다.** `for-ai/`를 참고.

---

## for-ai/

Claude/Codex가 작업 시 참조하는 문서 폴더.
**세션 시작 시 전부 로드하지 않고, 해당 작업에서만 호출한다.**

| 파일/폴더 | 호출 타이밍 |
|------|------------|
| `review.md` | PR 리뷰 작성 시 |
| `frontend/conventions.md` | 프론트엔드 코드 작성/수정 시 |
| `backend/conventions.md` | 백엔드 코드 작성/수정 시 |
| `ai-server/conventions.md` | AI 서버 코드 작성/수정 시 |
| `spec/_common.md` | 기능 스펙 파일과 항상 함께 로드 |
| `spec/summoner.md` | Summoner/Match 작업 시 |
| `spec/dashboard.md` | 메타 스냅샷 작업 시 |
| `spec/decks.md` | Deck 작업 시 |
| `spec/guide.md` | Guide 작업 시 |
| `spec/patch-notes.md` | PatchNotes 작업 시 |
| `spec/ai-recommend.md` | AI Recommend 작업 시 |
| `spec/admin.md` | Admin 작업 시 |
| `coderabbit-fix-prompt.md` | CodeRabbit 수정 작업 시 |
| `coderabbit-decision-workflow.md` | CodeRabbit 판단 흐름 확인 시 |

---

## ai-harness/

AI 도구에 팀 컨벤션을 주입하기 위한 하네스 문서.

| 파일 | 내용 |
|------|------|
| `AGENTS.md` | Codex가 저장소 전체에서 항상 참고하는 기본 작업 규칙 |
| `coderabbit-fix-prompt.md` | CodeRabbit 리뷰 코멘트 수정 전용 프롬프트 |

### CodeRabbit 수정 흐름

1. PR을 열고 CodeRabbit 리뷰를 받는다
2. 사용자가 코멘트에 `fix`, `skip`, `discuss`로 판단을 남긴다
3. Codex에게 `coderabbit-fix-prompt.md` 프롬프트를 실행시킨다
4. Codex는 `fix` 항목만 수정하고 검증한다
5. 기존 PR 브랜치에 push하거나, PR이 없으면 새 PR을 만든다

### 운영 원칙

- 컨벤션 원문은 Spring/FastAPI/React PDF와 기존 Claude Code 프롬프트를 기준으로 한다
- AI가 임의로 대규모 리팩터링하지 않도록 현재 diff와 리뷰 코멘트 범위 안에서 작업시킨다
