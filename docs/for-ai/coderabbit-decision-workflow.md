# CodeRabbit Decision Workflow

이 문서는 CodeRabbit 리뷰 코멘트를 사용자가 먼저 판단하고, Codex가 승인된 항목만 수정해 PR에 반영하는 흐름을 정의한다.

## 목표

- CodeRabbit 코멘트를 AI가 무조건 수정하지 않게 한다.
- 사용자가 `fix`, `skip`, `discuss`로 판단한 뒤 Codex가 그 판단을 따른다.
- 수정, 검증, 커밋, push, PR 생성 또는 기존 PR 업데이트까지 한 번의 작업 흐름으로 묶는다.

## PR 코멘트 프로토콜

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

CodeRabbit 코멘트 번호가 없으면 파일 경로와 요지를 사용한다.

```md
/codex fix-coderabbit

판단:
- fix: frontend/src/pages/Decks/Decks.tsx - any 타입 제거
- fix: backend/src/main/java/com/tftgogo/domain/member/service/impl/MemberServiceImpl.java - null 반환 제거
- skip: frontend/src/pages/Decks/Decks.module.css - 색상 톤 제안은 이번 PR 제외
```

## Codex 실행 프롬프트

```text
현재 PR의 CodeRabbit 리뷰 코멘트와 사용자가 남긴 /codex fix-coderabbit 판단 코멘트를 확인해줘.

규칙:
- AGENTS.md와 docs/ai-harness/coderabbit-fix-prompt.md를 기준으로 작업한다.
- fix 항목만 수정한다.
- skip/discuss 항목은 임의로 수정하지 않는다.
- 보안 위험, 빌드 실패, 명백한 런타임 오류가 skip되어 있으면 수정하지 말고 위험을 요약한다.
- 변경 후 가능한 검증 명령을 실행한다.
- 기존 PR이 있으면 같은 브랜치에 커밋 push한다.
- PR이 없으면 codex/coderabbit-fix-<작업명> 브랜치를 만들고 develop 대상으로 PR을 생성한다.

마지막에 다음을 한국어로 요약한다:
- 수정한 항목
- 보류한 항목
- 검증 결과
- push 또는 PR URL
```

## Codex가 수행할 절차

1. 현재 브랜치와 PR 번호를 확인한다.
2. CodeRabbit 리뷰 코멘트와 사용자의 `/codex fix-coderabbit` 판단 코멘트를 확인한다.
3. `fix` 항목을 파일 경로와 변경 의도로 매핑한다.
4. 관련 파일만 수정한다.
5. 프로젝트별 검증 명령을 실행한다.
6. 커밋 메시지는 `fix: CodeRabbit 리뷰 반영` 형식을 사용한다.
7. 기존 PR이면 현재 브랜치에 push한다.
8. PR이 없으면 `develop` 대상으로 새 PR을 만든다.

## GitHub Actions 자동화 한계

GitHub PR 코멘트만으로 Codex가 자동 실행되려면 별도 Codex 실행 환경, 인증 토큰, 저장소 쓰기 권한이 필요하다. 이 저장소의 하네스는 Codex 앱 또는 로컬 Codex 세션에서 사용자가 실행하는 흐름을 기준으로 한다.
