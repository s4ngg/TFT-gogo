# Guide — 게임 가이드

## 개요

시너지, 아이템, 증강체, 챔피언 기본 정보를 탭 단위로 탐색할 수 있는 페이지.

---

## 공통 기능

- 탭: 시너지 / 아이템 / 증강체 / 챔피언
- 각 탭은 키워드 검색을 제공한다
- 검색어 또는 필터 변경 시 목록은 첫 페이지로 초기화된다
- 즐겨찾기 및 최근 본 가이드로 자주 보는 정보를 빠르게 다시 열 수 있다

### 페이지네이션

| 탭 | 페이지당 개수 |
|----|------------|
| 시너지 | 6개 |
| 아이템 | 5개 |
| 증강체 | 5개 |
| 챔피언 | 10개 |

페이지 번호는 최대 5개까지 표시하고 이후는 다음/더보기 흐름으로 이동한다.

---

## 시너지 가이드

- 시너지별 이름, 유형, 활성 단계, 효과 설명을 확인할 수 있다
- 각 시너지의 필요 챔피언 목록을 확인할 수 있다
- 운영 팁 또는 주의점을 확인할 수 있다
- 시너지에 연결된 챔피언을 통해 챔피언 가이드로 이동할 수 있다

---

## 아이템 통계

- 아이템별 승률, TOP4 비율, 픽률, 평균 등수를 확인할 수 있다
- 추천 장착 챔피언, 조합 추천 및 사용 목적을 확인할 수 있다
- 승률 / TOP4 비율 / 픽률 / 평균 등수 기준으로 정렬할 수 있다

---

## 증강체 가이드

- 증강체별 티어, 승률, 픽률, 평균 등수, 설명을 확인할 수 있다
- 티어표 형태로 우선순위를 빠르게 비교할 수 있다
- 이름, 태그, 설명 기반 검색을 제공한다
- 증강체 보상표를 통해 스테이지/조건별 보상을 확인할 수 있다
- 운영 방향별 증강체 선택 흐름을 확인할 수 있다 (Fast 8 / 리롤 / 유연한 전환)

---

## 챔피언 가이드

- 챔피언별 이름, 코스트, 역할, 포지션, 시너지를 확인할 수 있다
- 카드 hover 시 체력/공격력/방어력/마법저항력/공격속도/사거리/마나 핵심 스탯 표시
- 챔피언별 3신기 아이템을 한 번에 확인할 수 있다
- 상세 모달에서 스탯, 시너지, 추천 아이템을 다시 확인할 수 있다
- 코스트(1~5) 기준 필터링을 제공한다

---

## API 계약

### 백엔드 엔드포인트

- `GET /api/guide` — 공통 메타 + 각 탭 대표 데이터 (`ApiResponse<List<GuideEntryResponse>>`)
- `GET /api/guide/{tab}` — 탭별 목록 (`ApiResponse<GuidePageResponse<GuideEntryResponse>>`)
  - `{tab}` 허용값: `traits` / `items` / `augments` / `champions`

### 쿼리 파라미터

| 파라미터 | 적용 탭 | 설명 |
|---------|--------|------|
| `query` | 전체 | 키워드 검색 |
| `page` | 전체 | 페이지 번호 (1부터 시작) |
| `pageSize` | 전체 | 페이지 크기 |
| `sortKey` | items, augments | `winRate` / `top4` / `pickRate` / `avgPlace` |
| `sortDir` | items, augments | `asc` / `desc` |
| `cost` | champions | 1~5. 전체 선택 시 생략 |

### 페이지 응답 형태

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "totalItems": 100,
  "totalPages": 10
}
```

Spring Page 응답 시 `items` 대신 `content` 허용.

### GuideEntryResponse 필드

`id`, `guideType`, `targetKey`, `name`, `summary`, `imageUrl`, `patchVersion`, `sortOrder`, `dataJson`

- `guideType` enum: `TRAIT` / `ITEM` / `AUGMENT` / `CHAMPION`
- `dataJson` 유형별 구조:
  - TRAIT: `type`, `count`, `levels`, `champions`, `tips`, `tone`
  - ITEM: `category`, `winRate`, `top4`, `pickRate`, `avgPlace`, `bestUsers`, `combinations`
  - AUGMENT: `tier`, `type`, `description`, `reward`, `tags`, `winRate`, `pickRate`, `avgPlace`
  - CHAMPION: `cost`, `role`, `position`, `traits`, `bestItems`, `stats`

---

## 프론트 구조

- `pages/Guide/Guide.tsx` — 페이지 조립 (헤더, 탭 컨트롤, 패널 연결만 담당)
- `pages/Guide/components/` — 탭별 View 컴포넌트
  - `GuideControls.tsx` / `GuideTabPanels.tsx`
  - `TraitGuideView.tsx` / `ItemStatsView.tsx` / `AugmentGuideView.tsx` / `ChampionGuideView.tsx`
  - `ChampionDetailDialog.tsx` / `GuideQuickAccess.tsx` / `GuideShared.tsx`
- `pages/Guide/hooks/useGuidePageState.ts` — 탭/검색어/즐겨찾기/최근 본 가이드 클라이언트 상태
- `hooks/useGuide.ts` — TanStack Query 서버 상태 (`useGuideCatalog`, `useGuideTabItems`)
- `api/guide.ts` — barrel export 진입점
  - `guideTypes.ts` / `guideClient.ts` / `guideNormalizers.ts` / `guideFallback.ts`

---

## 데이터 정책

- 시너지, 아이템, 증강체, 챔피언 기본 정보는 Community Dragon JSON 기준
- API 연동 전/오류 시 `guideFallback.ts` 샘플 데이터로 fallback
- 가이드 데이터는 관리자 큐레이션 기반으로 관리 (`/api/admin/guides`)
- 로컬 DB smoke 기준 테이블은 `guides`이며, `guideType + targetKey + patchVersion` 조합을 하나의 가이드 항목으로 본다
- `patchVersion` 생략 조회는 여러 패치 버전 데이터를 섞지 않는다
- 관리자 생성/수정 시 `dataJson`은 JSON object로 검증한다
- 관리자 삭제: 실제 삭제보다 `isActive = false` 또는 `deletedAt` 기반 soft delete 우선

---

## 접근성 요구사항

- 탭: `aria-selected` / 페이지네이션: `aria-current` / 즐겨찾기: `aria-pressed`
- 검색 입력: 현재 탭 기준 명시적 접근성 레이블 제공
- 챔피언 상세 모달: 열릴 때 초점 이동, 닫힐 때 트리거로 초점 복귀
- 이미지 로딩 실패 시 항목 유형에 맞는 fallback 이미지 표시
