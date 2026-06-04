# Frontend Spec-Kit

> 프론트엔드 개발의 단일 기준 문서 모음.
> 작업 시작 전 반드시 해당 섹션을 읽고, PR 올리기 전 checklist.md를 통과해야 한다.

---

## 파일 목록

| 파일 | 역할 |
| --- | --- |
| `constitution.md` | 기술 스택, 폴더 구조, 코딩 컨벤션, PR 규칙, 브랜치 전략 |
| `spec.md` | 페이지별 기능 요구사항 (무엇을 만드는가) |
| `checklist.md` | 기능별 완료 조건 (PR 머지 전 통과 필수) |
| `design-tokens.md` | 타이포그래피, 색상, 간격 등 디자인 토큰 상세값 |
| `common-layout-and-components.md` | 공통 컴포넌트·레이아웃 목록 및 분리 기준 |
| `riot-tft-mock-data.md` | Riot API mock 데이터 구조 및 프론트 확인 방법 |
| `tasks.md` | 기능별 구현 진행 상황 추적 |
| `plan.md` | 구현 계획 및 아키텍처 결정 사항 |
| `release-note-summoner-detail-ui.md` | 소환사 상세 UI 릴리즈 노트 |

---

## 읽는 순서

### 처음 합류하는 팀원

1. `constitution.md` — 기술 스택, 폴더 구조, PR 규칙 전체 파악
2. `design-tokens.md` — 색상·타이포그래피 기준값 숙지
3. `common-layout-and-components.md` — 공통 컴포넌트 목록 확인
4. `spec.md` — 서비스 전체 기능 요구사항 파악

### UI 작업 시

1. `spec.md` 해당 페이지 섹션 — 기능 요구사항 확인
2. `design-tokens.md` — 색상·폰트·간격 기준 확인
3. `checklist.md` 해당 기능 섹션 — 완료 조건 확인 후 작업 시작

### API 연동 작업 시

1. `spec.md` 해당 페이지 "데이터 요구사항" 섹션 — API 스펙 확인
2. `riot-tft-mock-data.md` — mock 데이터 구조 확인
3. `checklist.md` 해당 기능 섹션 — 연동 완료 조건 확인

---

## PR 올리기 전 필수 확인

- [ ] 작업 시작 전 `spec.md` 해당 기능 섹션 확인
- [ ] `checklist.md` 해당 기능 체크리스트 통과
- [ ] `constitution.md` PR 규칙 준수 (제목 형식, 라벨, 마일스톤)
