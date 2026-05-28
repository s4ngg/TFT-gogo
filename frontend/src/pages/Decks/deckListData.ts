import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import type { ChampionSummary, TraitSummary } from '../Dashboard/dashboardData'

export interface HeroAugmentDeck {
  hero: string
  augment: string
  recommended: boolean
  winRate: string
  avgPlace: string
  pickRate: string
  description: string
  tags: string[]
  traits: TraitSummary[]
  champions: ChampionSummary[]
}

export interface ArtifactUnit {
  name: string
  imageUrl: string
  frequency: string
  winRate: string
  avgImprovement: string
  top4: string
}

export interface ArtifactRec {
  itemName: string
  itemIcon: string
  units: ArtifactUnit[]
}

export const INITIAL_ARTIFACT_COUNT = 4

const itemIconUrls = {
  infinityEdge: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  warmogsArmor: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  rabadonsDeathcap: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex'),
  spearOfShojin: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  blueBuff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
  giantSlayer: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GiantSlayer.TFT_Set13.tex'),
  dragonsClaw: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex'),
  morellonomicon: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex'),
  ionicSpark: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex'),
  titansResolve: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_TitansResolve.TFT_Set13.tex'),
}

const champUrls = {
  jhin: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  kaisa: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  xayah: communityDragonAssetUrl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  ornn: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  illaoi: communityDragonAssetUrl('ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex'),
  rammus: communityDragonAssetUrl('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  aurelionSol: communityDragonAssetUrl('ASSETS/Characters/TFT17_AurelionSol/Skins/Base/Images/TFT17_AurelionSol_splash_tile_2.TFT_Set17.tex'),
  vex: communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  viktor: communityDragonAssetUrl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  sona: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  karma: communityDragonAssetUrl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  masterYi: communityDragonAssetUrl('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  azir: communityDragonAssetUrl('ASSETS/Characters/TFT17_Azir/Skins/Base/Images/TFT17_Azir_splash_tile_1.TFT_Set17.tex'),
  sejuani: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sejuani/Skins/Base/Images/TFT17_Sejuani_splash_tile_1.TFT_Set17.tex'),
  yasuo: communityDragonAssetUrl('ASSETS/Characters/TFT17_Yasuo/Skins/Base/Images/TFT17_Yasuo_splash_tile_1.TFT_Set17.tex'),
  lux: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lux/Skins/Base/Images/TFT17_Lux_splash_tile_1.TFT_Set17.tex'),
}

export const HERO_AUGMENT_DECKS: HeroAugmentDeck[] = [
  {
    hero: '진',
    augment: '마지막 공연',
    recommended: true,
    winRate: '63.2%',
    avgPlace: '2.71',
    pickRate: '2.8%',
    description: '4번째 공격 데미지가 폭발적으로 증가. 암흑의 별 6개 + 진 3성 조합 시 1등 확정급 화력 보장.',
    tags: ['하이리스크', '3성 필수', '암흑의 별 시너지'],
    traits: [],
    champions: [
      { name: '진', imageUrl: champUrls.jhin, stars: 3 },
      { name: '카이사', imageUrl: champUrls.kaisa, stars: 2 },
    ],
  },
  {
    hero: '아우렐리온 솔',
    augment: '우주의 폭발',
    recommended: true,
    winRate: '61.8%',
    avgPlace: '2.94',
    pickRate: '2.1%',
    description: '궁극기 발동 시 광역 폭발 피해가 3배 증가. 요새 덱과 조합해 생존하며 폭딜.',
    tags: ['장기전', '요새 시너지', '후반 캐리'],
    traits: [],
    champions: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, stars: 3 },
      { name: '빅토르', imageUrl: champUrls.viktor, stars: 2 },
    ],
  },
  {
    hero: '마스터 이',
    augment: '알파 스트라이크 강화',
    recommended: true,
    winRate: '59.5%',
    avgPlace: '3.12',
    pickRate: '3.4%',
    description: '알파 스트라이크 추가 타겟 +2, 치명타 시 쿨타임 초기화. 습격자 4 조합 시 무한 광역 공격.',
    tags: ['습격자 필수', '3성 추천', '중반 캐리'],
    traits: [],
    champions: [
      { name: '마스터 이', imageUrl: champUrls.masterYi, stars: 3 },
      { name: '자야', imageUrl: champUrls.xayah, stars: 2 },
    ],
  },
  {
    hero: '소나',
    augment: '하모닉 웨이브',
    recommended: true,
    winRate: '57.3%',
    avgPlace: '3.38',
    pickRate: '1.9%',
    description: '소나 스킬 사용 시 전 아군 체력 회복 + 공격력 증가. 정령족 + 우주 그루브와 시너지.',
    tags: ['힐 덱', '장기전', '정령족 조합'],
    traits: [],
    champions: [
      { name: '소나', imageUrl: champUrls.sona, stars: 3 },
      { name: '카르마', imageUrl: champUrls.karma, stars: 2 },
    ],
  },
  {
    hero: '아지르',
    augment: '황제의 군단',
    recommended: false,
    winRate: '48.2%',
    avgPlace: '4.31',
    pickRate: '1.2%',
    description: '황제 시너지 자원 요구량이 높아 안정적인 운영이 어렵고, 일반 덱 대비 효율이 낮음.',
    tags: ['고자원 필요', '별 3 필수', '우주 시너지'],
    traits: [],
    champions: [
      { name: '아지르', imageUrl: champUrls.azir, stars: 3 },
      { name: '빅토르', imageUrl: champUrls.viktor, stars: 2 },
    ],
  },
  {
    hero: '세주아니',
    augment: '빙하 폭풍',
    recommended: false,
    winRate: '45.7%',
    avgPlace: '4.68',
    pickRate: '0.9%',
    description: '발동 조건이 까다롭고 현 메타 카운터 아이템이 다수. 특정 조합에서만 제한적으로 유효.',
    tags: ['발동 불안정', '메타 불리', '탱커 필요'],
    traits: [],
    champions: [
      { name: '세주아니', imageUrl: champUrls.sejuani, stars: 3 },
      { name: '오른', imageUrl: champUrls.ornn, stars: 2 },
    ],
  },
  {
    hero: '야스오',
    augment: '허무의 검',
    recommended: false,
    winRate: '43.1%',
    avgPlace: '4.92',
    pickRate: '0.7%',
    description: '치명타 아이템 의존도가 높아 아이템 없이 불안정. 초반 골드 경쟁에서 불리한 포지션.',
    tags: ['아이템 의존', '불안정', '초반 불리'],
    traits: [],
    champions: [
      { name: '야스오', imageUrl: champUrls.yasuo, stars: 3 },
      { name: '진', imageUrl: champUrls.jhin, stars: 2 },
    ],
  },
  {
    hero: '럭스',
    augment: '최후의 섬광',
    recommended: false,
    winRate: '41.3%',
    avgPlace: '5.14',
    pickRate: '0.5%',
    description: '후반 캐리형이나 현 패치 스킬 쿨타임 너프로 효율 크게 감소. 패치 상황 지속 모니터링 필요.',
    tags: ['너프 대상', '후반 의존', '현 패치 비추'],
    traits: [],
    champions: [
      { name: '럭스', imageUrl: champUrls.lux, stars: 3 },
      { name: '벡스', imageUrl: champUrls.vex, stars: 2 },
    ],
  },
]

export const ARTIFACT_RECS: ArtifactRec[] = [
  {
    itemName: '무한의 대검',
    itemIcon: itemIconUrls.infinityEdge,
    units: [
      { name: '진', imageUrl: champUrls.jhin, frequency: '18.3%', winRate: '62.1%', avgImprovement: '+0.94', top4: '71.2%' },
      { name: '카이사', imageUrl: champUrls.kaisa, frequency: '14.1%', winRate: '58.4%', avgImprovement: '+0.71', top4: '66.8%' },
      { name: '자야', imageUrl: champUrls.xayah, frequency: '11.7%', winRate: '56.2%', avgImprovement: '+0.58', top4: '63.4%' },
    ],
  },
  {
    itemName: '워모그의 갑옷',
    itemIcon: itemIconUrls.warmogsArmor,
    units: [
      { name: '오른', imageUrl: champUrls.ornn, frequency: '22.5%', winRate: '64.8%', avgImprovement: '+1.12', top4: '74.3%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '17.8%', winRate: '60.3%', avgImprovement: '+0.87', top4: '69.1%' },
      { name: '람머스', imageUrl: champUrls.rammus, frequency: '13.2%', winRate: '57.9%', avgImprovement: '+0.64', top4: '65.7%' },
    ],
  },
  {
    itemName: '라바돈의 죽음모자',
    itemIcon: itemIconUrls.rabadonsDeathcap,
    units: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, frequency: '15.6%', winRate: '66.2%', avgImprovement: '+1.28', top4: '76.4%' },
      { name: '벡스', imageUrl: champUrls.vex, frequency: '12.4%', winRate: '61.7%', avgImprovement: '+0.95', top4: '70.8%' },
      { name: '빅토르', imageUrl: champUrls.viktor, frequency: '10.9%', winRate: '58.5%', avgImprovement: '+0.73', top4: '67.2%' },
    ],
  },
  {
    itemName: '쇼진의 창',
    itemIcon: itemIconUrls.spearOfShojin,
    units: [
      { name: '소나', imageUrl: champUrls.sona, frequency: '16.2%', winRate: '59.4%', avgImprovement: '+0.82', top4: '68.5%' },
      { name: '카르마', imageUrl: champUrls.karma, frequency: '13.7%', winRate: '57.1%', avgImprovement: '+0.67', top4: '65.1%' },
    ],
  },
  {
    itemName: '블루 버프',
    itemIcon: itemIconUrls.blueBuff,
    units: [
      { name: '소나', imageUrl: champUrls.sona, frequency: '19.4%', winRate: '65.3%', avgImprovement: '+1.18', top4: '75.2%' },
      { name: '카르마', imageUrl: champUrls.karma, frequency: '15.8%', winRate: '62.1%', avgImprovement: '+0.96', top4: '71.6%' },
      { name: '빅토르', imageUrl: champUrls.viktor, frequency: '12.3%', winRate: '58.7%', avgImprovement: '+0.74', top4: '67.5%' },
    ],
  },
  {
    itemName: '거인 슬레이어',
    itemIcon: itemIconUrls.giantSlayer,
    units: [
      { name: '진', imageUrl: champUrls.jhin, frequency: '16.7%', winRate: '61.2%', avgImprovement: '+0.91', top4: '70.3%' },
      { name: '마스터 이', imageUrl: champUrls.masterYi, frequency: '14.2%', winRate: '59.8%', avgImprovement: '+0.79', top4: '68.9%' },
      { name: '카이사', imageUrl: champUrls.kaisa, frequency: '11.8%', winRate: '57.4%', avgImprovement: '+0.63', top4: '65.8%' },
    ],
  },
  {
    itemName: '용의 발톱',
    itemIcon: itemIconUrls.dragonsClaw,
    units: [
      { name: '오른', imageUrl: champUrls.ornn, frequency: '20.1%', winRate: '63.1%', avgImprovement: '+1.05', top4: '73.2%' },
      { name: '람머스', imageUrl: champUrls.rammus, frequency: '16.4%', winRate: '60.7%', avgImprovement: '+0.88', top4: '70.1%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '13.5%', winRate: '55.3%', avgImprovement: '+0.52', top4: '63.9%' },
    ],
  },
  {
    itemName: '모렐로노미콘',
    itemIcon: itemIconUrls.morellonomicon,
    units: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, frequency: '14.8%', winRate: '64.5%', avgImprovement: '+1.15', top4: '74.8%' },
      { name: '벡스', imageUrl: champUrls.vex, frequency: '12.1%', winRate: '60.2%', avgImprovement: '+0.88', top4: '69.4%' },
      { name: '카르마', imageUrl: champUrls.karma, frequency: '9.8%', winRate: '56.8%', avgImprovement: '+0.61', top4: '64.7%' },
    ],
  },
  {
    itemName: '이온 스파크',
    itemIcon: itemIconUrls.ionicSpark,
    units: [
      { name: '벡스', imageUrl: champUrls.vex, frequency: '13.6%', winRate: '62.3%', avgImprovement: '+0.97', top4: '71.9%' },
      { name: '빅토르', imageUrl: champUrls.viktor, frequency: '11.3%', winRate: '60.5%', avgImprovement: '+0.85', top4: '70.2%' },
      { name: '카르마', imageUrl: champUrls.karma, frequency: '9.4%', winRate: '57.2%', avgImprovement: '+0.66', top4: '65.4%' },
    ],
  },
  {
    itemName: '타이탄의 결의',
    itemIcon: itemIconUrls.titansResolve,
    units: [
      { name: '오른', imageUrl: champUrls.ornn, frequency: '24.8%', winRate: '68.1%', avgImprovement: '+1.42', top4: '78.5%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '19.3%', winRate: '62.4%', avgImprovement: '+1.07', top4: '72.8%' },
      { name: '람머스', imageUrl: champUrls.rammus, frequency: '15.7%', winRate: '59.0%', avgImprovement: '+0.81', top4: '68.3%' },
    ],
  },
]
