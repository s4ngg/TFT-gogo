# Release Note — SummonerDetail UI

**버전** v0.2.0  
**날짜** 2026-05-26  
**브랜치** `feature/summoner-detail-ui` → `develop`  
**PR** #27  
**작업자** gungang1212-tech

---

## 개요

소환사 전적 상세 페이지(SummonerDetail)의 UI를 전면 구현했습니다.  
소환사 카드, 시너지·챔피언 통계, 30게임 요약, 전적 목록, 게임별 8인 상세 패널까지 단일 페이지에서 제공합니다.

---

## 변경 파일

| 파일 | 변경 유형 |
|------|-----------|
| `src/pages/SummonerDetail/SummonerDetail.tsx` | 전면 재작성 |
| `src/pages/SummonerDetail/SummonerDetail.module.css` | 전면 재작성 |

---

## 신규 기능

### 소환사 카드
- 프로필 아이콘, 소환사명, 태그, 레벨
- 티어 / LP / 승수·패수 / 승률 / 평균 순위 / TOP4율
- **순위 분포 바 차트**: 1위~8위별 게임 수 시각화 (TOP4 청록 / 5~8위 레드)
- 전적 업데이트 버튼

### 많이 플레이한 시너지 / 챔피언

- 시너지·챔피언별 **게임 수 · 평균 등수** 통계 목록
- Community Dragon CDN 이미지 연동

### 최근 30게임 요약

- **순방확률 도넛 차트** — CSS `conic-gradient` 사용 (외부 라이브러리 없음)
- **순방확률**: TOP4 = W, 5위 이하 = L로 정의
  - 표시 형식: `20W 10L (66.7%)`
- **평균 순위**: `3.6th / 8`

### 전적 목록

- 순위 `1위`~`8위` 형식 + 순위별 색상 (1위 금색 / 2~4위 청록 / 5~8위 레드)
- 덱 이름, 경과 시간 (LP 변동값은 Riot API 미제공으로 표시하지 않음)
- **유닛 이미지**: CDN 기반, 3성 골드 테두리
- **아이템 이미지**: 챔피언당 최대 3개
- **호버 툴팁**: 챔피언·아이템 이미지 위에 커서를 올리면 이름 표시  
  → CSS `::after` + `data-tip` 속성 방식 (라이브러리 없음)
- **30개씩 더보기** 버튼 (남은 개수 표시)

### 전적 행 상세 펼치기

전적 행 클릭 시 해당 게임 8명 전원 정보가 인라인으로 펼쳐집니다.

| 컬럼 | 내용 |
|------|------|
| # | 최종 순위 (1위 금색, 2위 은색, 3위 동색, 4위 이하 기본색) |
| 소환사 | 소환사명 + 태그 |
| 스테이지 | last_round(int) 기반으로 Spring에서 변환한 스테이지 (예: 5 → 2-1) |
| 시너지 | 활성 시너지 아이콘 + 개수 (소환사 카드 "많이 플레이한 시너지"와 동일한 CDN 이미지 소스) |
| 챔피언 | 유닛 이미지 + 아이템 이미지 |
| 킬 | 킬 수 (Swords 아이콘) |
| 잔여골드 | 게임 종료 시 남은 골드 (Coins 아이콘, 금색) |

- 증강 컬럼 없음 (Riot API 공식 스키마 미포함 — 미제공 확정)
- 내 행은 청록 하이라이트로 구분
- 펼치기 / 접기 Chevron 아이콘

---

## 변경 사항 (이전 대비)

| 항목 | 이전 | 이후 |
|------|------|------|
| 순위 표시 | 숫자 + TOP4 / BOT 구분 | `1위` ~ `8위` 형식 |
| 승률 지표 명칭 | TOP4율 | 순방확률 (W/L) |
| 전적 목록 LP 변동 | 표시 예정 | 미제공 (Riot API 미지원) |
| 상세 패널 마지막 컬럼 | LP 증감 | 킬수 + 잔여골드 (별도 컬럼) |
| 전적 로드 방식 | 전체 노출 | 30개 기본, 더보기로 추가 로드 |

---

## 기술 참고

- **이미지**: Community Dragon CDN (`communityDragonAssets.ts`)
  - 챔피언: `ASSETS/Characters/TFT17_*/Skins/Base/Images/*.TFT_Set17.tex`
  - 아이템: `ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_*.TFT_Set13.tex`
  - 시너지: `ASSETS/UX/TraitIcons/Trait_Icon_*.tex`
- **툴팁**: CSS `[data-tip]::after` pseudo-element
- **도넛 차트**: CSS `conic-gradient` + CSS 변수 `--pct`
- **스타일**: CSS Modules (`SummonerDetail.module.css`), Tailwind 사용 안 함

---

## 미완료 (다음 PR 예정)

- 로딩 / 빈 상태 / 에러 상태 UI
- API 실데이터 연동 (PR 7)

## 확정 사항 (스펙 변경)

- 증강: 표시하지 않음 (Riot API 공식 스키마 미포함 — 미제공 확정)
- 상세 패널 순위 색상: 1위 금색, 2위 은색, 3위 동색 추가
- 시너지 아이콘: 소환사 카드 섹션과 동일한 CDN 이미지 소스 공유
