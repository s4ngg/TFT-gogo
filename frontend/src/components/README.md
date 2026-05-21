# Shared Frontend Components

팀원 페이지에서 반복 사용될 UI 컴포넌트를 이 폴더에 둡니다.

## 폴더 기준

| 폴더 | 용도 |
| --- | --- |
| `common/` | 페이지와 무관하게 반복되는 작은 UI 컴포넌트 |
| `layout/` | 사이드바, 상단바, 앱 프레임처럼 여러 페이지가 공유하는 레이아웃 |

페이지 한 곳에서만 쓰는 컴포넌트는 `pages/<PageName>/components`에 둡니다.
두 페이지 이상에서 쓰이기 시작하면 `components/common` 또는 `components/layout`으로 올립니다.

## 현재 공용 컴포넌트

| 컴포넌트 | 경로 | 용도 |
| --- | --- | --- |
| `TierBadge` | `components/common/TierBadge` | 메타 티어 `S`, `A+`, `A` 등급 배지 |
| `ChampionCard` | `components/common/ChampionCard` | TFT 챔피언 카드, 별 등급, 추천 아이템 슬롯 |
| `TraitHexBadge` | `components/common/TraitHexBadge` | 시너지/특성 육각 배지 |

## 기준

- 텍스트가 바뀌거나 데이터 값에 따라 달라지는 UI는 이미지가 아니라 컴포넌트로 만든다.
- 실제 게임 이미지가 필요한 챔피언/아이템/시너지/증강체는 Community Dragon CDN 매핑을 사용한다.
- 브랜드 심볼, 마스코트, 랭크 엠블럼처럼 그림 자체가 중요한 요소만 `public/assets` 이미지로 관리한다.
- 공통 레이아웃은 `components/layout`에서 관리하고 페이지별 본문만 `pages` 아래에서 조립한다.
- 스타일은 컨벤션에 따라 CSS Modules를 사용하고 Tailwind는 사용하지 않는다.

## 사용 예시

```tsx
import TierBadge from '../components/common/TierBadge'

<TierBadge value="S" />
```
