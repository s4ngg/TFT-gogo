# styles

디자인 토큰(색상·크기·간격)은 `src/styles/variables.css`에서만 관리하고, `src/index.css`는 reset·폰트 import만 담는다. 화면/컴포넌트별 스타일은 CSS Modules를 사용한다.

- CSS Modules 클래스명은 camelCase를 사용한다.
- Tailwind는 사용하지 않는다.
- 색상·크기 하드코딩 금지 — 반드시 `variables.css`의 `var(--토큰명)` 사용.
