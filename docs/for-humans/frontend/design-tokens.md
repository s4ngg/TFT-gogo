# Frontend Design Tokens

기준 화면은 `etc/최종.png`입니다.
폰트와 숫자, 간격이 깨지면 전체 인상이 바로 달라지기 때문에 팀원 페이지에서도 아래 값을 우선 기준으로 맞춥니다.

## Font

```css
@import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css");
```

```css
font-family: "Pretendard", -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
font-feature-settings: "tnum";
font-variant-numeric: tabular-nums;
```

## Typography Scale

| 용도 | 크기 | 굵기 | 자간 |
| --- | ---: | ---: | ---: |
| 로고 텍스트 | 25px | 800 | -0.04em |
| 메인 검색 제목 | 25px | 600 | 0.045em |
| 섹션 제목 | 21px | 600 | -0.024em |
| 우측 카드 제목 | 21px | 600 | -0.024em |
| AI 카드 제목 | 18px | 600 | -0.024em |
| 패치 카드 제목 | 20px | 600 | -0.024em |
| 메뉴 텍스트 | 16px | 500 | -0.015em |
| 활성 메뉴 텍스트 | 16px | 600 | -0.015em |
| 덱명 | 15px | 600 | -0.015em |
| 버튼 텍스트 | 14px | 600 | -0.01em |
| 본문 설명 | 13px | 400~500 | -0.01em |
| 작은 라벨 | 10~12px | 500~600 | -0.01em |
| 승률/TOP4 숫자 | 15px | 600 | 기본 |

## Line And Wrapping Rules

- 제목, 버튼, 탭, 덱명, 날짜 텍스트는 `white-space: nowrap`을 우선 적용합니다.
- 긴 덱명이나 검색 태그처럼 공간이 좁아질 수 있는 값은 `overflow: hidden`, `text-overflow: ellipsis`를 같이 사용합니다.
- 숫자는 `tabular-nums`를 적용해 폭이 흔들리지 않게 합니다.
- 한국어 UI는 `word-break: keep-all`을 우선 사용합니다.

## Colors

> 모든 색상은 `src/styles/variables.css`에 CSS 변수로 정의하고 `var(--토큰명)`으로 참조한다.
> 컴포넌트 CSS에 hex, rgb, rgba 색상값 직접 하드코딩 금지.
> 새 색상이 필요하면 variables.css에 토큰을 먼저 추가한 후 참조한다.
> 예외: box-shadow의 rgba 흰/검 그림자 깊이값은 의미 재사용이 없을 경우 하드코딩 허용.

| CSS 변수명 | 값 | 용도 |
| --- | --- | --- |
| `--bg-main` | `#070d14` | 전체 배경 |
| `--bg-sidebar` | `#050a10` | 사이드바 배경 |
| `--bg-card` | `#0b1420` | 카드 기본 배경 |
| `--bg-card-soft` | `#101a27` | 부드러운 카드 배경 |
| `--bg-card-gradient` | `linear-gradient(180deg, rgba(22,37,56,.96), rgba(14,25,38,.96))` | 카드/패널 배경(페이지 배경과 명도 구분) |
| `--border` | `#1f2a37` | 기본 테두리 |
| `--border-active` | `#00d4b4` | 활성/포커스 테두리 |
| `--border-card-highlight` | `rgba(255,255,255,.14)` | 카드/패널 외곽 강조 테두리(흰색) |
| `--color-cyan` | `#05f3e7` | 활성 아이콘, CTA, 주요 버튼 |
| `--color-cyan-num` | `#04ede0` | 승률 강조 숫자 |
| `--color-gold` | `#f7d26d` | 랭크 1~2, 티어 배지 |
| `--color-red` | `#ff4545` | 경고, Bot4 |
| `--color-success` | `#4ade80` | 성공 상태 |
| `--text-main` | `#ffffff` | 기본 텍스트 |
| `--text-soft` | `#c6ccd8` | 보조 텍스트 |
| `--text-muted` | `#8b92a8` | 흐린 텍스트 |

> 위 표는 핵심 토큰만 정리한 것입니다. 전체 토큰 목록(패널 배경, 매치 행 색상, 배지, 툴팁 등)은
> `src/styles/variables.css`를 단일 소스로 참조하세요.
> 추가된 토큰 prefix: `--bg-*` `--border-*` `--cyan-*` `--match-*` `--badge-*` `--search-*` `--donut-*`

## Radius And Borders

| CSS 변수명 | 값 | 용도 |
| --- | ---: | --- |
| `--radius-card` | `12px` | 카드 |
| `--radius-button` | `8px` | 버튼, 탭, 메타 테이블 내부 |
| _(없음)_ | `6px` | 챔피언 카드, 작은 아이콘 |

- 페이지 배경에 바로 맞닿는 카드/패널(전적 카드, 전적 상세 프로필/통계 카드, 대시보드 패널 등)은
  `border: 2px solid var(--border-card-highlight)` + `background: var(--bg-card-gradient)`를 기본값으로 사용합니다.
- 강조색(금색/보라색 등) 테두리가 의미를 가지는 카드(랭크 카드, 메타 덱 카드 등)와
  중첩된 하위 패널(매치 상세 패널 등)은 예외로, 기존 강조색·배경을 유지합니다.

## Layout Density

- 전체 홈 화면은 넓은 랜딩 페이지가 아니라 게임 대시보드입니다.
- 카드 간격은 12~15px 안에서 유지합니다.
- 사이드바 폭은 222px 기준입니다.
- 홈 대시보드 본문 기준 폭은 현재 1307px로 맞춰져 있습니다.
- 브라우저 확대/축소 시 카드 내부 내용이 깨지지 않도록 고정 최소 폭과 `nowrap`을 같이 사용합니다.
