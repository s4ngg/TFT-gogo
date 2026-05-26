# Riot TFT Mock Data Guide

프론트 React 작업은 Spring API 구현 전까지 mock data로 진행한다.
Riot API key는 프론트에 두지 않고 Spring에서만 관리한다.

## 파일 위치

- Riot 원본 응답 mock: `frontend/src/mocks/riotTftMockData.ts`
- React/Spring DTO mock: `frontend/src/mocks/deckResponseMock.ts`
- 덱모음 API fallback: `frontend/src/api/deckApi.ts`

## Riot 원본 흐름

Spring은 Riot API에서 아래 데이터를 가져와 가공한다.

1. `tft-summoner-v1`
   - 소환사 id, puuid, profileIconId, summonerLevel

2. `tft-league-v1`
   - tier, rank, leaguePoints, wins, losses

3. `tft-match-v1`
   - match id 목록
   - match 상세의 participants
   - participant의 placement, traits, units, augments

## React가 받을 덱모음 DTO 예시

`GET /api/decks/meta`

```ts
type DeckMetaResponse = {
  rank: number
  grade: 'S' | 'A+' | 'A' | 'B' | 'C' | 'D'
  name: string
  winRate: string
  top4: string
  avgPlace: string
  pickRate: string
  traits: {
    name: string
    count: number
    iconUrl: string
    tone: 'bronze' | 'silver' | 'gold' | 'prismatic'
  }[]
  champions: {
    name: string
    imageUrl: string
    stars: 1 | 2 | 3
    items?: {
      name: string
      imageUrl: string
    }[]
  }[]
}[]
```

## 가공 기준

- `placement`으로 평균 등수, 승률, TOP4 비율 계산
- `traits`의 `name`, `num_units`, `style`을 시너지 표시로 변환
- `units`의 `character_id`, `tier`, `itemNames`를 챔피언 카드로 변환
- 챔피언/아이템/시너지 이미지 URL은 Data Dragon 또는 Community Dragon asset helper로 변환

## 프론트 확인 방법

Spring 없이도 `deckApi.getMetaDecks()`가 실패하면 `mockDeckMetaResponse`를 반환한다.

```bash
cd frontend
npm.cmd install
npm.cmd run dev
```

브라우저에서 확인:

```text
http://localhost:5173/decks
```

## 참고

- Riot TFT API: https://developer.riotgames.com/docs/tft
- Riot API key는 React `.env`에 넣지 않는다.
