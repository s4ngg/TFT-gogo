import { communityDragonAssetUrl } from '../api/communityDragonAssets'
import type { AiRecommendResponse } from '../api/aiRecommendApi'

export const mockAiRecommendation: AiRecommendResponse = {
  stats: {
    recentGames: 20,
    avgPlace: '4.1',
    top4Rate: '58.0%',
    winRate: '22.5%',
    recentPlacements: [1, 4, 4, 3, 2, 6, 1, 5, 3, 2, 3, 1, 7, 4, 2, 5, 3, 4, 6, 1],
  },
  goodTraits: [
    {
      name: '선봉대', count: 4,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
      tone: 'gold', games: 12, avgPlace: '3.2', top4Rate: '72%',
    },
    {
      name: '정령족', count: 4,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Astronaut.TFT_Set17.tex'),
      tone: 'gold', games: 8, avgPlace: '3.7', top4Rate: '65%',
    },
    {
      name: '암흑의 별', count: 6,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex'),
      tone: 'gold', games: 5, avgPlace: '3.9', top4Rate: '61%',
    },
  ],
  badTraits: [
    {
      name: '초능력', count: 3,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_PsyOps.TFT_Set17.tex'),
      tone: 'silver', games: 6, avgPlace: '5.4', top4Rate: '24%',
    },
    {
      name: '복제자', count: 2,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Replicator.TFT_Set17.tex'),
      tone: 'silver', games: 5, avgPlace: '5.9', top4Rate: '21%',
    },
    {
      name: '습격자', count: 4,
      iconUrl: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
      tone: 'silver', games: 5, avgPlace: '6.1', top4Rate: '19%',
    },
  ],
  augments: [
    { name: '강철의 의지', avgPlace: '2.9', games: 4, icon: '🛡️' },
    { name: '정의의 손길+', avgPlace: '3.2', games: 3, icon: '⚔️' },
    { name: '용의 불꽃', avgPlace: '3.5', games: 5, icon: '🔥' },
    { name: '별의 수호자', avgPlace: '4.8', games: 3, icon: '✨' },
    { name: '전사의 용기', avgPlace: '5.1', games: 4, icon: '🗡️' },
  ],
  deckReasons: [
    { deckRank: 1, isPatchTrend: false, reason: '내가 자주 쓰는 챔피언 포함' },
    { deckRank: 2, isPatchTrend: false, reason: '현재 메타 최상위 티어' },
    { deckRank: 3, isPatchTrend: true, reason: '패치 후 픽률 + TOP4 확률 증가!' },
  ],
}
