# Frontend Asset Policy

TFTgogo는 실제 TFT 유저가 보는 서비스이므로, 게임과 직접 관련된 이미지는 가능한 한 실제 게임 에셋을 사용합니다.

## 저장소에 직접 둘 이미지

| 분류 | 이유 |
| --- | --- |
| TFTgogo 브랜드 심볼 | 서비스 자체 로고라 직접 관리 필요 |
| Riot API 카드 마스코트 배경 | 최종 시안과 맞춘 홈 전용 일러스트 |
| 랭크 엠블럼 | 사이드바 랭크 카드의 고정 장식 |
| 패치 추천 메타 분홍 엠블럼 | 홈 상단 카드 전용 장식 |

## 현재 repo에 포함해야 하는 이미지 파일

팀원이 pull 받았을 때 로컬에서 바로 보여야 하는 고정 이미지는 `frontend/public/assets` 아래에 둡니다.
Vite 기준으로 이 폴더의 파일은 앱에서 `/assets/...` 경로로 접근합니다.

| repo 파일 | 앱에서 쓰는 경로 | 사용 위치 |
| --- | --- | --- |
| `frontend/public/assets/brand/tftgogo-mark.png` | `/assets/brand/tftgogo-mark.png` | 사이드바 브랜드 아이콘 |
| `frontend/public/assets/illustrations/riot-api-mascot-card.png` | `/assets/illustrations/riot-api-mascot-card.png` | 홈 `Riot API 연동` 카드 |
| `frontend/public/assets/emblems/patch-meta-emblem-pink.png` | `/assets/emblems/patch-meta-emblem-pink.png` | 홈 `17.3 추천 메타` 카드 |
| `frontend/public/assets/ranks/platinum-emblem.png` | `/assets/ranks/platinum-emblem.png` | 사이드바 랭크 카드 |

이 파일들은 PR에 포함되어야 하며, `dist` 빌드 산출물이나 `node_modules`는 커밋하지 않습니다.

## Community Dragon CDN으로 불러올 이미지

| 분류 | 이유 |
| --- | --- |
| 챔피언 이미지 | 실제 게임 이미지와 가장 가까워야 함 |
| 아이템 이미지 | 추천 덱에서 장착 아이템을 보여줘야 함 |
| 시너지/특성 아이콘 | 메타 스냅샷과 게임 가이드에서 반복 사용 |
| 증강체 아이콘 | AI 추천, 게임 가이드에서 반복 사용 가능 |
| 프로필 아이콘 | 로그인 프로필 이미지 후보 |

## 구현 기준

- Community Dragon URL helper는 `frontend/src/api/communityDragonAssets.ts`에서 관리합니다.
- 챔피언/아이템/특성 이미지를 컴포넌트 내부에 하드코딩하지 않고 데이터에서 주입합니다.
- 여러 페이지에서 같은 이미지를 쓸 수 있는 경우 저장소에 중복 저장하지 않습니다.
- 커스텀 제작 이미지가 필요하면 `frontend/public/assets` 아래에 분류별 폴더를 만들고 이 문서와 `frontend/public/assets/README.md`에 기록합니다.

## 파티원 찾기 아이콘 기준

파티원 찾기 카드의 보라/초록/청록/골드 아이콘은 실제 챔피언이나 아이템이 아니라 모집 유형을 빠르게 구분하는 UI 배지입니다.
그래서 Community Dragon 이미지보다 CSS + lucide 아이콘 조합이 적합합니다.

다만 게임 가이드, 덱 추천, 메타 스냅샷처럼 실제 TFT 정보를 보여주는 영역은 Community Dragon 이미지를 우선 사용합니다.
