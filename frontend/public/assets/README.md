# TFTgogo Shared Image Assets

이 폴더는 프론트 팀원이 함께 쓰는 공용 이미지 에셋 저장소입니다.

## 폴더 규칙

| 폴더 | 용도 | repo 포함 |
| --- | --- | --- |
| `brand/` | TFTgogo 자체 로고, 심볼, 워드마크 | 포함 |
| `illustrations/` | 홈 카드용 마스코트, 장식 일러스트 | 포함 |
| `emblems/` | 자체 제작 배지, 랭크 느낌 아이콘, 패치 카드 아이콘 | 포함 |
| `ranks/` | 자체 제작 랭크 엠블럼 | 포함 |

## 현재 사용 중인 에셋

| 파일 | 사용 위치 | 코드 경로 |
| --- | --- | --- |
| `brand/tftgogo-mark.png` | 사이드바 브랜드 아이콘 | `/assets/brand/tftgogo-mark.png` |
| `illustrations/riot-api-mascot-card.png` | 홈 `Riot API 연동` 카드 | `/assets/illustrations/riot-api-mascot-card.png` |
| `emblems/patch-meta-emblem-pink.png` | 홈 `17.3 추천 메타` 카드 | `/assets/emblems/patch-meta-emblem-pink.png` |
| `ranks/platinum-emblem.png` | 사이드바 랭크 카드 | `/assets/ranks/platinum-emblem.png` |

## 외부 게임 데이터 에셋 규칙

- 실제 챔피언, 아이템, 시너지, 증강체 이미지는 repo에 직접 넣지 않습니다.
- 해당 이미지는 Community Dragon CDN/JSON 매핑으로 가져오는 것을 기준으로 합니다.
- 프론트에서는 `src/api/communityDragonAssets.ts`의 `communityDragonAssetUrl()`을 통해 URL을 만들고, 홈 메타 스냅샷은 `ChampionCard`, `TraitHexBadge` 공통 컴포넌트에서 사용합니다.
- 로컬에 저장하는 것은 TFTgogo 자체 제작 이미지와 UI 장식 에셋만 허용합니다.
- 시안, 실패본, 비교용 이미지는 `etc/asset-drafts` 또는 `etc/visual-checks`에 둡니다.
- 파티원 찾기 카드의 색상 아이콘은 실제 게임 데이터가 아니라 모집 유형을 나타내는 UI 배지이므로 이미지 파일이 아닌 CSS/Lucide 기반 컴포넌트로 관리합니다.

## 네이밍 규칙

- 소문자 kebab-case를 사용합니다.
- 파일명만 보고 위치와 용도를 알 수 있게 작성합니다.
- 예: `tftgogo-mark.png`, `riot-api-mascot-card.png`, `patch-meta-emblem-pink.png`

## 사용 예시

```css
background: url("/assets/brand/tftgogo-mark.png") center / contain no-repeat;
```

```tsx
<img src="/assets/illustrations/riot-api-mascot-card.png" alt="" />
```
