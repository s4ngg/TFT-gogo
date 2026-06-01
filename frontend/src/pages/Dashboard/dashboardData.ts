import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import type { ChampionCardProps } from '../../components/common/ChampionCard'
import type { TierBadgeValue, TraitHexBadgeTone } from '../../types/badges'

export type RankFilter = 'EMERALD_PLUS' | 'DIAMOND_PLUS' | 'MASTER_PLUS'

export interface ItemSummary {
  itemId: string
  itemName: string
  playRate: string
  winRate: string
  placementDelta: string
}

export interface MetaDeck {
  rank: number
  grade: TierBadgeValue
  name: string
  winRate: string
  top4: string
  avgPlace: string
  pickRate: string
  sampleSize?: number               // 집계에 사용된 경기 수 (소규모면 통계 신뢰도 낮음)
  traits: TraitSummary[]
  champions: ChampionSummary[]
  topItems?: ItemSummary[]          // 덱별 추천 아이템 (API 집계 후 제공)
}

export interface TraitSummary {
  count: number
  iconUrl: string
  name: string
  tone: TraitHexBadgeTone
}

export interface ChampionSummary {
  imageUrl: string
  items?: ChampionItemSummary[]
  name: string
  stars: ChampionCardProps['stars']
  cost?: number
  recommendedItems?: string[]   // API 실데이터: 추천 아이템 ID 목록
}

export interface ChampionItemSummary {
  imageUrl: string
  name: string
}

export interface PartyPost {
  title: string
  mode: string
  tier: string
  count: string
  close: string
  icon: 'crown' | 'leaf' | 'spark' | 'goal'
  tone: 'purple' | 'green' | 'cyan' | 'gold'
}

export interface ChatChannel {
  name: string
  users: string
  message: string
  time: string
}

const traitIconUrls = {
  animaSquad: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_AnimaTech.TFT_Set17.tex'),
  challenger: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  darkStar: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex'),
  fateweaver: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Fateweaver.TFT_Set17.tex'),
  bastion: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_9_Bastion.tex'),
  mech: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Mecha.TFT_Set17.tex'),
  nova: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_NOVA.TFT_Set17.tex'),
  psyOps: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_PsyOps.TFT_Set17.tex'),
  replicator: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Replicator.TFT_Set17.tex'),
  rogue: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  shepherd: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Shepherd.TFT_Set17.tex'),
  sniper: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_6_Sniper.tex'),
  spaceGroove: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_SpaceGroove.TFT_Set17.tex'),
  stargazer: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
  spirit: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Astronaut.TFT_Set17.tex'),
  vanguard: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
}

const championImageUrls = {
  akali: communityDragonAssetUrl('ASSETS/Characters/TFT17_Akali/Skins/Base/Images/TFT17_Akali_splash_tile_68.TFT_Set17.tex'),
  aurelionSol: communityDragonAssetUrl('ASSETS/Characters/TFT17_AurelionSol/Skins/Base/Images/TFT17_AurelionSol_splash_tile_2.TFT_Set17.tex'),
  aurora: communityDragonAssetUrl('ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex'),
  bard: communityDragonAssetUrl('ASSETS/Characters/TFT17_Bard/Skins/Base/Images/TFT17_Bard_splash_tile_8.TFT_Set17.tex'),
  belveth: communityDragonAssetUrl('ASSETS/Characters/TFT17_Belveth/Skins/Base/Images/TFT17_Belveth_splash_tile_19.TFT_Set17.tex'),
  blitzcrank: communityDragonAssetUrl('ASSETS/Characters/TFT17_Blitzcrank/Skins/Base/Images/TFT17_Blitzcrank_splash_tile_65.TFT_Set17.tex'),
  briar: communityDragonAssetUrl('ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex'),
  corki: communityDragonAssetUrl('ASSETS/Characters/TFT17_Corki/Skins/Base/Images/TFT17_Corki_splash_tile_26.TFT_Set17.tex'),
  galio: communityDragonAssetUrl('ASSETS/Characters/TFT17_Galio/Skins/Base/Images/TFT17_Galio_Mobile.TFT_Set17.tex'),
  gnar: communityDragonAssetUrl('ASSETS/Characters/TFT17_Gnar/Skins/Base/Images/TFT17_Gnar_splash_tile_15.TFT_Set17.tex'),
  illaoi: communityDragonAssetUrl('ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex'),
  jax: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jax/Skins/Base/Images/TFT17_Jax_Mobile.TFT_Set17.tex'),
  jhin: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  jinx: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  kaisa: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  karma: communityDragonAssetUrl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  lulu: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lulu/Skins/Base/Images/TFT17_Lulu_splash_tile_14.TFT_Set17.tex'),
  masterYi: communityDragonAssetUrl('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  morgana: communityDragonAssetUrl('ASSETS/Characters/TFT17_Morgana/Skins/Base/Images/TFT17_Morgana_splash_tile_50.TFT_Set17.tex'),
  nami: communityDragonAssetUrl('ASSETS/Characters/TFT17_Nami/Skins/Base/Images/TFT17_Nami_splash_tile_41.TFT_Set17.tex'),
  ornn: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  poppy: communityDragonAssetUrl('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  pyke: communityDragonAssetUrl('ASSETS/Characters/TFT17_Pyke/Skins/Base/Images/TFT17_Pyke_splash_tile_25.TFT_Set17.tex'),
  rammus: communityDragonAssetUrl('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  riven: communityDragonAssetUrl('ASSETS/Characters/TFT17_Riven/Skins/Base/Images/TFT17_Riven_splash_tile_18.TFT_Set17.tex'),
  shen: communityDragonAssetUrl('ASSETS/Characters/TFT17_Shen/Skins/Base/Images/TFT17_shen_splash_tile_49.TFT_Set17.tex'),
  sona: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  twistedFate: communityDragonAssetUrl('ASSETS/Characters/TFT17_TwistedFate/Skins/Base/Images/TFT17_TwistedFate_splash_tile_45.TFT_Set17.tex'),
  vex: communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  viktor: communityDragonAssetUrl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  xayah: communityDragonAssetUrl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  zed: communityDragonAssetUrl('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
}

const itemIconUrls = {
  adaptiveHelm: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_AdaptiveHelm.TFT_Set13.tex'),
  archangelsStaff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_ArchangelsStaff.TFT_Set13.tex'),
  bloodthirster: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Bloodthirster.TFT_Set13.tex'),
  blueBuff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
  crownguard: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Crownguard.TFT_Set13.tex'),
  dragonsClaw: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex'),
  gargoyleStoneplate: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GargoyleStoneplate.TFT_Set13.tex',
  ),
  guinsoosRageblade: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex',
  ),
  handOfJustice: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_UnstableConcoction.TFT_Set13.tex'),
  infinityEdge: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  ionicSpark: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex'),
  jeweledGauntlet: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_JeweledGauntlet.TFT_Set13.tex'),
  lastWhisper: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_LastWhisper.TFT_Set13.tex'),
  morellonomicon: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex'),
  rabadonsDeathcap: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex',
  ),
  spearOfShojin: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  steraksGage: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SteraksGage.TFT_Set13.tex'),
  titansResolve: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_TitansResolve.TFT_Set13.tex'),
  warmogsArmor: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
}

function trait(name: string, count: number, iconUrl: string, tone: TraitHexBadgeTone = 'gold'): TraitSummary {
  return { count, iconUrl, name, tone }
}

function item(name: string, imageUrl: string): ChampionItemSummary {
  return { imageUrl, name }
}

const recommendedItems = {
  assassin: [
    item('무한의 대검', itemIconUrls.infinityEdge),
    item('정의의 손길', itemIconUrls.handOfJustice),
    item('피바라기', itemIconUrls.bloodthirster),
  ],
  bruiser: [
    item('피바라기', itemIconUrls.bloodthirster),
    item('거인의 결의', itemIconUrls.titansResolve),
    item('스테락의 도전', itemIconUrls.steraksGage),
  ],
  magicBurst: [
    item('푸른 파수꾼', itemIconUrls.blueBuff),
    item('보석 건틀릿', itemIconUrls.jeweledGauntlet),
    item('라바돈의 죽음모자', itemIconUrls.rabadonsDeathcap),
  ],
  magicRamp: [
    item('쇼진의 창', itemIconUrls.spearOfShojin),
    item('대천사의 지팡이', itemIconUrls.archangelsStaff),
    item('모렐로노미콘', itemIconUrls.morellonomicon),
  ],
  magicUtility: [
    item('푸른 파수꾼', itemIconUrls.blueBuff),
    item('모렐로노미콘', itemIconUrls.morellonomicon),
    item('보석 건틀릿', itemIconUrls.jeweledGauntlet),
  ],
  physicalCarry: [
    item('구인수의 격노검', itemIconUrls.guinsoosRageblade),
    item('무한의 대검', itemIconUrls.infinityEdge),
    item('최후의 속삭임', itemIconUrls.lastWhisper),
  ],
  support: [
    item('쇼진의 창', itemIconUrls.spearOfShojin),
    item('모렐로노미콘', itemIconUrls.morellonomicon),
    item('적응형 투구', itemIconUrls.adaptiveHelm),
  ],
  tank: [
    item('워모그의 갑옷', itemIconUrls.warmogsArmor),
    item('가고일 돌갑옷', itemIconUrls.gargoyleStoneplate),
    item('용의 발톱', itemIconUrls.dragonsClaw),
  ],
  utility: [
    item('정의의 손길', itemIconUrls.handOfJustice),
    item('이온 충격기', itemIconUrls.ionicSpark),
    item('크라운가드', itemIconUrls.crownguard),
  ],
}

function champion(name: string, imageUrl: string, stars: ChampionSummary['stars'], items?: ChampionItemSummary[]): ChampionSummary {
  return { imageUrl, items, name, stars }
}

export const metaDecks: MetaDeck[] = [
  {
    rank: 1,
    grade: 'S',
    name: '선봉대 벡스',
    winRate: '55.8%',
    top4: '72.6%',
    avgPlace: '3.84',
    pickRate: '11.2%',
    traits: [
      trait('선봉대', 4, traitIconUrls.vanguard),
      trait('동물특공대', 3, traitIconUrls.animaSquad),
      trait('파멸자', 2, traitIconUrls.darkStar, 'silver'),
      trait('별돌보미', 2, traitIconUrls.stargazer),
      trait('정령족', 2, traitIconUrls.spirit),
      trait('요새', 1, traitIconUrls.bastion, 'silver'),
    ],
    champions: [
      champion('벡스', championImageUrls.vex, 3),
      champion('일라오이', championImageUrls.illaoi, 2),
      champion('자야', championImageUrls.xayah, 2),
      champion('오로라', championImageUrls.aurora, 3, recommendedItems.magicBurst),
      champion('뽀삐', championImageUrls.poppy, 2, recommendedItems.tank),
      champion('브라이어', championImageUrls.briar, 2, recommendedItems.bruiser),
    ],
  },
  {
    rank: 2,
    grade: 'S',
    name: '6암흑의 별 진',
    winRate: '54.3%',
    top4: '75.8%',
    avgPlace: '3.52',
    pickRate: '9.8%',
    traits: [
      trait('암흑의 별', 6, traitIconUrls.darkStar),
      trait('말살자', 2, traitIconUrls.sniper),
      trait('불한당', 2, traitIconUrls.rogue, 'silver'),
      trait('도전자', 2, traitIconUrls.challenger),
      trait('초능력', 2, traitIconUrls.psyOps),
      trait('저격수', 1, traitIconUrls.sniper, 'silver'),
    ],
    champions: [
      champion('진', championImageUrls.jhin, 3),
      champion('카이사', championImageUrls.kaisa, 2),
      champion('카르마', championImageUrls.karma, 2),
      champion('모르가나', championImageUrls.morgana, 3, recommendedItems.magicBurst),
      champion('벨베스', championImageUrls.belveth, 2, recommendedItems.bruiser),
      champion('파이크', championImageUrls.pyke, 2, recommendedItems.utility),
    ],
  },
  {
    rank: 3,
    grade: 'A+',
    name: '정령족 코르키 백류',
    winRate: '52.1%',
    top4: '71.8%',
    avgPlace: '3.92',
    pickRate: '8.5%',
    traits: [
      trait('정령족', 4, traitIconUrls.spirit),
      trait('운명술사', 3, traitIconUrls.fateweaver),
      trait('도전자', 2, traitIconUrls.challenger, 'silver'),
      trait('복제자', 2, traitIconUrls.replicator),
      trait('초능력', 2, traitIconUrls.psyOps),
      trait('길잡이', 1, traitIconUrls.shepherd, 'silver'),
    ],
    champions: [
      champion('코르키', championImageUrls.corki, 3),
      champion('벡스', championImageUrls.vex, 2),
      champion('오로라', championImageUrls.aurora, 2),
      champion('바드', championImageUrls.bard, 3, recommendedItems.magicRamp),
      champion('소나', championImageUrls.sona, 2, recommendedItems.magicUtility),
      champion('나르', championImageUrls.gnar, 2, recommendedItems.tank),
    ],
  },
  {
    rank: 4,
    grade: 'A',
    name: '습격자 마스터 이',
    winRate: '51.0%',
    top4: '70.4%',
    avgPlace: '3.38',
    pickRate: '7.9%',
    traits: [
      trait('습격자', 4, traitIconUrls.rogue),
      trait('초능력', 3, traitIconUrls.psyOps),
      trait('요새', 2, traitIconUrls.bastion, 'silver'),
      trait('은하계 사냥꾼', 2, traitIconUrls.nova),
      trait('도전자', 2, traitIconUrls.challenger),
      trait('불한당', 1, traitIconUrls.rogue, 'silver'),
    ],
    champions: [
      champion('마스터 이', championImageUrls.masterYi, 3),
      champion('파이크', championImageUrls.pyke, 2),
      champion('쉔', championImageUrls.shen, 2),
      champion('제드', championImageUrls.zed, 3, recommendedItems.physicalCarry),
      champion('벨베스', championImageUrls.belveth, 2, recommendedItems.bruiser),
      champion('아칼리', championImageUrls.akali, 2, recommendedItems.assassin),
    ],
  },
  {
    rank: 5,
    grade: 'A',
    name: '별돌보미 룰루 (메탈)',
    winRate: '49.6%',
    top4: '76.4%',
    avgPlace: '3.49',
    pickRate: '7.1%',
    traits: [
      trait('별돌보미', 4, traitIconUrls.stargazer),
      trait('정령족', 3, traitIconUrls.spirit),
      trait('복제자', 2, traitIconUrls.replicator, 'silver'),
      trait('운명술사', 2, traitIconUrls.fateweaver),
      trait('요새', 2, traitIconUrls.bastion),
      trait('저격수', 1, traitIconUrls.sniper, 'silver'),
    ],
    champions: [
      champion('룰루', championImageUrls.lulu, 3),
      champion('뽀삐', championImageUrls.poppy, 2),
      champion('나미', championImageUrls.nami, 2),
      champion('트페', championImageUrls.twistedFate, 3, recommendedItems.magicBurst),
      champion('잭스', championImageUrls.jax, 2, recommendedItems.bruiser),
      champion('자야', championImageUrls.xayah, 2, recommendedItems.physicalCarry),
    ],
  },
  {
    rank: 6,
    grade: 'A',
    name: '8요새 럼블',
    winRate: '48.2%',
    top4: '74.1%',
    avgPlace: '3.44',
    pickRate: '6.4%',
    traits: [
      trait('요새', 8, traitIconUrls.bastion),
      trait('정령족', 2, traitIconUrls.spirit),
      trait('우주 그루브', 2, traitIconUrls.spaceGroove),
      trait('메카', 2, traitIconUrls.mech),
      trait('복제자', 2, traitIconUrls.replicator),
    ],
    champions: [
      champion('람머스', championImageUrls.rammus, 3),
      champion('뽀삐', championImageUrls.poppy, 2),
      champion('오른', championImageUrls.ornn, 2),
      champion('아우솔', championImageUrls.aurelionSol, 3, recommendedItems.magicRamp),
      champion('블리츠', championImageUrls.blitzcrank, 2, recommendedItems.tank),
      champion('나미', championImageUrls.nami, 2, recommendedItems.support),
    ],
  },
  {
    rank: 7,
    grade: 'A',
    name: '4그림자 암살자',
    winRate: '47.1%',
    top4: '64.0%',
    avgPlace: '4.31',
    pickRate: '5.8%',
    traits: [
      trait('불한당', 6, traitIconUrls.rogue),
      trait('N.O.V.A.', 4, traitIconUrls.nova),
      trait('요새', 2, traitIconUrls.bastion, 'silver'),
      trait('저격수', 2, traitIconUrls.sniper),
    ],
    champions: [
      champion('쉔', championImageUrls.shen, 3),
      champion('아칼리', championImageUrls.akali, 2),
      champion('제드', championImageUrls.zed, 2),
      champion('파이크', championImageUrls.pyke, 3, recommendedItems.assassin),
      champion('리븐', championImageUrls.riven, 2, recommendedItems.bruiser),
      champion('진', championImageUrls.jhin, 2, recommendedItems.physicalCarry),
    ],
  },
  {
    rank: 8,
    grade: 'A',
    name: '발명의 대가 하이머딩거',
    winRate: '46.3%',
    top4: '73.5%',
    avgPlace: '3.61',
    pickRate: '5.2%',
    traits: [
      trait('초능력', 5, traitIconUrls.psyOps),
      trait('운명술사', 2, traitIconUrls.fateweaver, 'silver'),
      trait('메카', 2, traitIconUrls.mech),
      trait('도전자', 2, traitIconUrls.challenger),
      trait('복제자', 2, traitIconUrls.replicator),
    ],
    champions: [
      champion('빅토르', championImageUrls.viktor, 3),
      champion('코르키', championImageUrls.corki, 2),
      champion('아우솔', championImageUrls.aurelionSol, 2),
      champion('징크스', championImageUrls.jinx, 3, recommendedItems.physicalCarry),
      champion('소나', championImageUrls.sona, 2, recommendedItems.magicUtility),
      champion('나미', championImageUrls.nami, 2, recommendedItems.support),
    ],
  },
  {
    rank: 9,
    grade: 'B',
    name: '4저격수 징크스',
    winRate: '45.1%',
    top4: '61.8%',
    avgPlace: '4.55',
    pickRate: '4.7%',
    traits: [
      trait('저격수', 4, traitIconUrls.sniper),
      trait('암흑의 별', 2, traitIconUrls.darkStar, 'silver'),
      trait('도전자', 2, traitIconUrls.challenger),
      trait('습격자', 2, traitIconUrls.rogue, 'silver'),
    ],
    champions: [
      champion('징크스', championImageUrls.jinx, 3, recommendedItems.physicalCarry),
      champion('카이사', championImageUrls.kaisa, 2),
      champion('진', championImageUrls.jhin, 2),
      champion('모르가나', championImageUrls.morgana, 2),
      champion('나미', championImageUrls.nami, 2, recommendedItems.support),
      champion('뽀삐', championImageUrls.poppy, 2),
    ],
  },
  {
    rank: 10,
    grade: 'B',
    name: '복제자 빅토르',
    winRate: '44.0%',
    top4: '60.2%',
    avgPlace: '4.63',
    pickRate: '4.1%',
    traits: [
      trait('복제자', 4, traitIconUrls.replicator),
      trait('메카', 3, traitIconUrls.mech),
      trait('초능력', 2, traitIconUrls.psyOps),
      trait('요새', 2, traitIconUrls.bastion, 'silver'),
    ],
    champions: [
      champion('빅토르', championImageUrls.viktor, 3, recommendedItems.magicRamp),
      champion('아우솔', championImageUrls.aurelionSol, 2),
      champion('코르키', championImageUrls.corki, 2),
      champion('오른', championImageUrls.ornn, 2, recommendedItems.tank),
      champion('블리츠', championImageUrls.blitzcrank, 2),
      champion('소나', championImageUrls.sona, 2),
    ],
  },
  {
    rank: 11,
    grade: 'C',
    name: '우주그루브 소나',
    winRate: '42.4%',
    top4: '57.9%',
    avgPlace: '4.82',
    pickRate: '3.3%',
    traits: [
      trait('우주 그루브', 4, traitIconUrls.spaceGroove),
      trait('정령족', 2, traitIconUrls.spirit),
      trait('별돌보미', 2, traitIconUrls.stargazer, 'silver'),
      trait('운명술사', 2, traitIconUrls.fateweaver),
    ],
    champions: [
      champion('소나', championImageUrls.sona, 3, recommendedItems.magicUtility),
      champion('룰루', championImageUrls.lulu, 2),
      champion('나미', championImageUrls.nami, 2),
      champion('바드', championImageUrls.bard, 2),
      champion('뽀삐', championImageUrls.poppy, 2, recommendedItems.tank),
      champion('나르', championImageUrls.gnar, 2),
    ],
  },
  {
    rank: 12,
    grade: 'C',
    name: '운명술사 트페',
    winRate: '41.2%',
    top4: '56.1%',
    avgPlace: '4.97',
    pickRate: '2.9%',
    traits: [
      trait('운명술사', 4, traitIconUrls.fateweaver),
      trait('도전자', 3, traitIconUrls.challenger, 'silver'),
      trait('초능력', 2, traitIconUrls.psyOps),
      trait('불한당', 2, traitIconUrls.rogue),
    ],
    champions: [
      champion('트페', championImageUrls.twistedFate, 3, recommendedItems.magicBurst),
      champion('아칼리', championImageUrls.akali, 2),
      champion('파이크', championImageUrls.pyke, 2),
      champion('쉔', championImageUrls.shen, 2),
      champion('나르', championImageUrls.gnar, 2, recommendedItems.tank),
      champion('모르가나', championImageUrls.morgana, 2),
    ],
  },
  {
    rank: 13,
    grade: 'D',
    name: '6도전자 자야',
    winRate: '39.5%',
    top4: '53.4%',
    avgPlace: '5.18',
    pickRate: '2.2%',
    traits: [
      trait('도전자', 6, traitIconUrls.challenger),
      trait('암흑의 별', 2, traitIconUrls.darkStar, 'silver'),
      trait('습격자', 2, traitIconUrls.rogue),
    ],
    champions: [
      champion('자야', championImageUrls.xayah, 3, recommendedItems.physicalCarry),
      champion('카이사', championImageUrls.kaisa, 2),
      champion('진', championImageUrls.jhin, 2),
      champion('아칼리', championImageUrls.akali, 2),
      champion('잭스', championImageUrls.jax, 2, recommendedItems.bruiser),
      champion('제드', championImageUrls.zed, 2),
    ],
  },
  {
    rank: 14,
    grade: 'D',
    name: '길잡이 나미',
    winRate: '38.1%',
    top4: '51.7%',
    avgPlace: '5.34',
    pickRate: '1.8%',
    traits: [
      trait('길잡이', 4, traitIconUrls.shepherd),
      trait('정령족', 2, traitIconUrls.spirit, 'silver'),
      trait('별돌보미', 2, traitIconUrls.stargazer),
    ],
    champions: [
      champion('나미', championImageUrls.nami, 3, recommendedItems.support),
      champion('룰루', championImageUrls.lulu, 2),
      champion('소나', championImageUrls.sona, 2),
      champion('바드', championImageUrls.bard, 2),
      champion('오로라', championImageUrls.aurora, 2, recommendedItems.magicBurst),
      champion('뽀삐', championImageUrls.poppy, 2),
    ],
  },
]

export const partyPosts: PartyPost[] = [
  {
    title: '마스터 이상 듀오 구합니다',
    mode: '랭크',
    tier: '마스터+',
    count: '2/2',
    close: '마감 15분 전',
    icon: 'crown',
    tone: 'purple',
  },
  {
    title: '다이아 구간 야부/연습 같이해요',
    mode: '랭크',
    tier: '다이아+',
    count: '1/2',
    close: '마감 42분 전',
    icon: 'leaf',
    tone: 'green',
  },
  {
    title: '저녁 근접, 편하게 즐기실 분!',
    mode: '일반',
    tier: '제한 없음',
    count: '3/4',
    close: '마감 1시간 전',
    icon: 'spark',
    tone: 'cyan',
  },
  {
    title: '주말 마스터 달성 목표!',
    mode: '랭크',
    tier: '플래티넘+',
    count: '2/3',
    close: '마감 2시간 전',
    icon: 'goal',
    tone: 'gold',
  },
]

export const chatChannels: ChatChannel[] = [
  { name: '일반', users: '1,234', message: '새로운 패치 적응 중입니다!', time: '14:58' },
  { name: '덱 공략', users: '856', message: '증강 추천 부탁드려요', time: '14:57' },
  { name: '자유 채팅', users: '2,102', message: '오늘 운 진짜 좋다 ㅋㅋ', time: '14:57' },
  { name: '파티 모집', users: '622', message: '마스터 듀오 구해요~', time: '14:56' },
  { name: '질문 & 답변', users: '741', message: '초보 운영 질문 있습니다', time: '14:56' },
  { name: '아이템 토론', users: '489', message: '쇼진 먼저 잡는 판이 많네요', time: '14:55' },
  { name: '증강 연구', users: '1,018', message: '전투 증강 첫 선택 괜찮나요?', time: '14:55' },
  { name: '초보방', users: '334', message: '연패 운영 언제 끊어야 해요?', time: '14:54' },
  { name: '랭크 후기', users: '913', message: '암흑 별 진 2성 찍으니 안정적', time: '14:54' },
  { name: '친선 모집', users: '276', message: '커스텀 한 판 하실 분?', time: '14:53' },
  { name: '패치 분석', users: '645', message: '선봉대 밸류가 아직 높네요', time: '14:53' },
  { name: '운영 질문', users: '528', message: '레벨업 타이밍 조언 부탁드려요', time: '14:52' },
]
