# Common Layout And Components

프론트 컨벤션 문서의 구조는 다음 기준입니다.

```txt
frontend/src/
├── pages/
├── components/
├── store/
├── hooks/
├── api/
└── styles/
```

따라서 공통 UI는 페이지 폴더 안에 계속 두지 않고 `components` 아래로 올리는 것이 맞습니다.

## 추천 패키지 구조

```txt
frontend/src/components/
├── common/
│   ├── ChampionCard/
│   ├── TierBadge/
│   └── TraitHexBadge/
└── layout/
    ├── README.md
    ├── AppLayout.tsx
    ├── Sidebar.tsx
    └── TopBar.tsx
```

## 분리 기준

| 위치 | 넣을 것 | 넣지 말 것 |
| --- | --- | --- |
| `components/common` | 배지, 카드, 아이콘 래퍼처럼 여러 화면에서 반복되는 작은 UI | 특정 페이지의 API 호출, 페이지 전용 데이터 |
| `components/layout` | 사이드바, 상단바, 앱 shell, 공통 네비게이션 | 메타 스냅샷 본문, 파티원 찾기 본문처럼 페이지 전용 위젯 |
| `pages/<PageName>` | 페이지별 본문, 해당 페이지에서만 쓰는 섹션 컴포넌트 | 두 페이지 이상에서 반복되는 UI |
| `api` | Riot API, Community Dragon URL helper, axios 인스턴스 | JSX 컴포넌트 |
| `hooks` | React Query hook, 페이지 공용 hook | 단순 UI 컴포넌트 |
| `store` | 로그인 사용자, 연동 소환사 등 전역 상태 | 페이지 지역 상태 |

## 현재 공통화된 컴포넌트

| 컴포넌트 | 용도 |
| --- | --- |
| `ChampionCard` | 챔피언 이미지, 별 등급, 추천 아이템 슬롯 |
| `TierBadge` | S, A, B 같은 메타 등급 배지 |
| `TraitHexBadge` | 시너지/특성 육각 배지 |

## 현재 공통화된 레이아웃

| 컴포넌트 | 포함 내용 |
| --- | --- |
| `Sidebar` | 로고, 메뉴, 랭크 카드, 전적 업데이트, 의견 보내기 |
| `TopBar` | 알림, 메일, 도움말, 프로필 |
| `AppLayout` | 사이드바와 상단바를 포함한 앱 공통 shell |

사용 예시는 다음과 같습니다.

```tsx
import { AppLayout } from '../../components/layout'

function DecksPage() {
  return (
    <AppLayout>
      <section>페이지 본문</section>
    </AppLayout>
  )
}
```

## 페이지 작업 방식

팀원이 자기 페이지를 만들 때는 다음 방식으로 맞춥니다.

```txt
pages/Decks/
├── Decks.tsx
├── Decks.module.css
├── decksData.ts
└── components/
```

- 페이지 전용 스타일은 `PageName.module.css`에 둡니다.
- 공통 배지/카드/레이아웃이 필요하면 `components` 아래 공통 컴포넌트를 가져옵니다.
- Tailwind class를 섞지 않습니다.
- 새 공통 컴포넌트를 만들면 이 문서와 `frontend/src/components/README.md`에 같이 기록합니다.
