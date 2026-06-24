# AI Harness

이 폴더는 TFT-gogo 팀 컨벤션을 AI 도구에 주입하기 위한 하네스 문서를 보관한다.

- `AGENTS.md`: Codex가 저장소 전체에서 항상 참고하는 기본 작업 규칙
- `coderabbit-fix-prompt.md`: CodeRabbit 리뷰 코멘트 수정 전용 프롬프트

## 사용 예시

```text
@coderabbit 현재 PR 리뷰 코멘트와 사용자가 남긴 /codex fix-coderabbit 판단 코멘트를 확인해줘.
fix 항목만 TFT-gogo 컨벤션 기준으로 수정하고 skip/discuss 항목은 보류해.
수정 후 검증하고 기존 PR 브랜치에 커밋 push하거나 PR이 없으면 새 PR을 만들어줘.
```

## 사용자 판단 기반 수정 흐름

1. PR을 열고 CodeRabbit 리뷰를 받는다.
2. 사용자가 CodeRabbit 코멘트를 보고 `fix`, `skip`, `discuss`로 판단 코멘트를 남긴다.
3. Codex에게 `docs/ai-harness/coderabbit-fix-prompt.md`의 짧은 호출용 프롬프트를 실행시킨다.
4. Codex는 `fix` 항목만 수정하고 검증한다.
5. 기존 PR이 있으면 같은 브랜치에 push하고, PR이 없으면 `develop` 대상으로 새 PR을 만든다.

판단 코멘트 예시:

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

## 운영 원칙

- 컨벤션 원문은 Spring/FastAPI/React PDF와 기존 Claude Code 프롬프트를 기준으로 한다.
- 실제 저장소는 React + TypeScript를 사용하므로 React 예시는 `.tsx/.ts` 기준으로 보정한다.
- AI가 임의로 대규모 리팩터링하지 않도록 현재 diff와 리뷰 코멘트 범위 안에서 작업시킨다.
- 완전한 GitHub 서버 자동 실행은 별도 Codex 실행 환경과 토큰이 필요하다. 이 하네스는 로컬 Codex 또는 Codex 앱에서 사용자가 실행하는 자동 수정 흐름을 기준으로 한다.
