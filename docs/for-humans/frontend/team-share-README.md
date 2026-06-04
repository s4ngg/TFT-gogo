# TFTgogo Frontend Team Share

이 폴더는 팀원에게 공유할 프론트 공통 기준 문서입니다.
개인 PC 경로가 아니라 레포 안의 이 폴더를 기준으로 공유합니다.

## 읽는 순서

1. `design-tokens.md`
   - 폰트, 자간, 크기, 색상, radius, 정보 밀도 기준
2. `common-layout-and-components.md`
   - 사이드바, 상단바, 네비게이션, 공통 컴포넌트 분리 기준
3. `asset-policy.md`
   - 이미지 파일로 둘 것과 Community Dragon CDN으로 불러올 것

## 현재 합의된 구현 방향

- 기준 이미지는 `etc/최종.png`입니다.
- 목표는 이미지 한 장을 그대로 박는 방식이 아니라, 실제 React DOM으로 첫 화면 인상과 정보 밀도를 맞추는 방식입니다.
- 프론트 컨벤션은 Vite + React + TypeScript + CSS Modules 기준입니다.
- Tailwind는 컨벤션상 사용하지 않습니다.
- 팀원 페이지는 공통 레이아웃과 공통 컴포넌트를 재사용하고, 자기 담당 본문만 페이지 폴더에서 구현합니다.

## 공유 방법

- 팀 채팅에는 이 폴더 위치만 공유합니다: `etc/team-share/frontend/`
- PR 설명에는 이 폴더의 문서를 기준 문서로 링크합니다.
- 스크린샷만 공유하지 말고, 폰트/크기/자간/색상 기준을 함께 공유합니다.
