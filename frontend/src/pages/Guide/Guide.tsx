import { useEffect, useMemo, useState } from 'react'
import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Gem,
  Package,
  Rows3,
  Search,
  Shield,
  Sparkles,
  Star,
  Swords,
  Trophy,
  X,
} from 'lucide-react'
import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import TierBadge, { type TierBadgeValue } from '../../components/common/TierBadge'
import TraitHexBadge, { type TraitHexBadgeTone } from '../../components/common/TraitHexBadge'
import { AppLayout } from '../../components/layout'
import styles from './Guide.module.css'

type GuideTab = 'traits' | 'items' | 'augments' | 'champions'
type ChampionCostFilter = 'all' | 1 | 2 | 3 | 4 | 5
type AugmentPlanKey = 'fast8' | 'reroll' | 'flex'
type MetricSortKey = 'avgPlace' | 'pickRate' | 'top4' | 'winRate'
type SortDir = 'asc' | 'desc'

const DEFAULT_GUIDE_PAGE_SIZE = 5
const TRAIT_PAGE_SIZE = 6
const CHAMPION_PAGE_SIZE = 10
const SAMPLE_PAGE_COUNT = 7
const PAGE_NUMBER_WINDOW = 5
const SAMPLE_VARIANTS = ['운영', '고점', '안정', '전환', '리롤', '연승', '후반']

interface ChampionRef {
  cost: number
  imageUrl: string
  name: string
}

interface ItemRef {
  imageUrl: string
  name: string
}

interface TraitGuide {
  champions: ChampionRef[]
  count: number
  iconUrl: string
  levels: string[]
  name: string
  summary: string
  tips: string[]
  tone?: TraitHexBadgeTone
  type: string
}

interface ItemStatGuide {
  avgPlace: string
  bestUsers: ChampionRef[]
  category: string
  combinations: {
    items: ItemRef[]
    label: string
    note: string
  }[]
  imageUrl: string
  name: string
  pickRate: string
  top4: string
  winRate: string
}

interface AugmentGuide {
  avgPlace: string
  description: string
  name: string
  pickRate: string
  reward: string
  tags: string[]
  tier: TierBadgeValue
  type: '실버' | '골드' | '프리즘'
  winRate: string
}

interface RewardRow {
  condition: string
  reward: string
  stage: string
}

interface AugmentPlan {
  key: AugmentPlanKey
  label: string
  stages: {
    choice: string
    focus: string
    stage: string
  }[]
}

interface ChampionGuide {
  bestItems: ItemRef[]
  cost: 1 | 2 | 3 | 4 | 5
  imageUrl: string
  name: string
  position: string
  role: string
  stats: {
    ad: number
    armor: number
    attackSpeed: string
    hp: number
    mana: string
    mr: number
    range: number
  }
  traits: string[]
}

interface RecentGuide {
  label: string
  query: string
  tab: GuideTab
}

interface SortableMetricItem {
  avgPlace: string
  pickRate: string
  top4?: string
  winRate: string
}

const traitIconUrls = {
  animaSquad: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_AnimaTech.TFT_Set17.tex'),
  bastion: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_9_Bastion.tex'),
  challenger: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  darkStar: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex'),
  fateweaver: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Fateweaver.TFT_Set17.tex'),
  nova: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_NOVA.TFT_Set17.tex'),
  rogue: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  spirit: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Astronaut.TFT_Set17.tex'),
  stargazer: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
  vanguard: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
}

const championUrls = {
  akali: communityDragonAssetUrl('ASSETS/Characters/TFT17_Akali/Skins/Base/Images/TFT17_Akali_splash_tile_68.TFT_Set17.tex'),
  aurelionSol: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_AurelionSol/Skins/Base/Images/TFT17_AurelionSol_splash_tile_2.TFT_Set17.tex',
  ),
  aurora: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex',
  ),
  bard: communityDragonAssetUrl('ASSETS/Characters/TFT17_Bard/Skins/Base/Images/TFT17_Bard_splash_tile_8.TFT_Set17.tex'),
  belveth: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Belveth/Skins/Base/Images/TFT17_Belveth_splash_tile_19.TFT_Set17.tex',
  ),
  blitzcrank: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Blitzcrank/Skins/Base/Images/TFT17_Blitzcrank_splash_tile_65.TFT_Set17.tex',
  ),
  briar: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex',
  ),
  corki: communityDragonAssetUrl('ASSETS/Characters/TFT17_Corki/Skins/Base/Images/TFT17_Corki_splash_tile_26.TFT_Set17.tex'),
  illaoi: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex',
  ),
  jhin: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  jinx: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  kaisa: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  karma: communityDragonAssetUrl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  lulu: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lulu/Skins/Base/Images/TFT17_Lulu_splash_tile_14.TFT_Set17.tex'),
  masterYi: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex',
  ),
  nami: communityDragonAssetUrl('ASSETS/Characters/TFT17_Nami/Skins/Base/Images/TFT17_Nami_splash_tile_41.TFT_Set17.tex'),
  ornn: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  poppy: communityDragonAssetUrl('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  pyke: communityDragonAssetUrl('ASSETS/Characters/TFT17_Pyke/Skins/Base/Images/TFT17_Pyke_splash_tile_25.TFT_Set17.tex'),
  rammus: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex',
  ),
  shen: communityDragonAssetUrl('ASSETS/Characters/TFT17_Shen/Skins/Base/Images/TFT17_shen_splash_tile_49.TFT_Set17.tex'),
  sona: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  vex: communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  viktor: communityDragonAssetUrl(
    'ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex',
  ),
  xayah: communityDragonAssetUrl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  zed: communityDragonAssetUrl('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
}

const itemUrls = {
  adaptiveHelm: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_AdaptiveHelm.TFT_Set13.tex'),
  archangelsStaff: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_ArchangelsStaff.TFT_Set13.tex',
  ),
  bloodthirster: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Bloodthirster.TFT_Set13.tex'),
  blueBuff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
  dragonsClaw: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex'),
  gargoyleStoneplate: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GargoyleStoneplate.TFT_Set13.tex',
  ),
  guinsoosRageblade: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex',
  ),
  handOfJustice: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_UnstableConcoction.TFT_Set13.tex'),
  infinityEdge: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  jeweledGauntlet: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_JeweledGauntlet.TFT_Set13.tex'),
  lastWhisper: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_LastWhisper.TFT_Set13.tex'),
  morellonomicon: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex'),
  rabadonsDeathcap: communityDragonAssetUrl(
    'ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex',
  ),
  spearOfShojin: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  titansResolve: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_TitansResolve.TFT_Set13.tex'),
  warmogsArmor: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
}

const itemRefs = {
  adaptiveHelm: item('적응형 투구', itemUrls.adaptiveHelm),
  archangelsStaff: item('대천사의 지팡이', itemUrls.archangelsStaff),
  bloodthirster: item('피바라기', itemUrls.bloodthirster),
  blueBuff: item('푸른 파수꾼', itemUrls.blueBuff),
  dragonsClaw: item('용의 발톱', itemUrls.dragonsClaw),
  gargoyleStoneplate: item('가고일 돌갑옷', itemUrls.gargoyleStoneplate),
  guinsoosRageblade: item('구인수의 격노검', itemUrls.guinsoosRageblade),
  handOfJustice: item('정의의 손길', itemUrls.handOfJustice),
  infinityEdge: item('무한의 대검', itemUrls.infinityEdge),
  jeweledGauntlet: item('보석 건틀릿', itemUrls.jeweledGauntlet),
  lastWhisper: item('최후의 속삭임', itemUrls.lastWhisper),
  morellonomicon: item('모렐로노미콘', itemUrls.morellonomicon),
  rabadonsDeathcap: item('라바돈의 죽음모자', itemUrls.rabadonsDeathcap),
  spearOfShojin: item('쇼진의 창', itemUrls.spearOfShojin),
  titansResolve: item('거인의 결의', itemUrls.titansResolve),
  warmogsArmor: item('워모그의 갑옷', itemUrls.warmogsArmor),
}

function champion(name: string, imageUrl: string, cost: number): ChampionRef {
  return { cost, imageUrl, name }
}

function item(name: string, imageUrl: string): ItemRef {
  return { imageUrl, name }
}

const championRefs = {
  akali: champion('아칼리', championUrls.akali, 2),
  aurelionSol: champion('아우렐리온 솔', championUrls.aurelionSol, 4),
  aurora: champion('오로라', championUrls.aurora, 3),
  bard: champion('바드', championUrls.bard, 5),
  belveth: champion('벨베스', championUrls.belveth, 2),
  blitzcrank: champion('블리츠크랭크', championUrls.blitzcrank, 5),
  briar: champion('브라이어', championUrls.briar, 1),
  corki: champion('코르키', championUrls.corki, 4),
  illaoi: champion('일라오이', championUrls.illaoi, 3),
  jhin: champion('진', championUrls.jhin, 5),
  jinx: champion('징크스', championUrls.jinx, 2),
  kaisa: champion('카이사', championUrls.kaisa, 3),
  karma: champion('카르마', championUrls.karma, 4),
  lulu: champion('룰루', championUrls.lulu, 3),
  masterYi: champion('마스터 이', championUrls.masterYi, 4),
  nami: champion('나미', championUrls.nami, 4),
  ornn: champion('오른', championUrls.ornn, 4),
  poppy: champion('뽀삐', championUrls.poppy, 1),
  pyke: champion('파이크', championUrls.pyke, 2),
  rammus: champion('람머스', championUrls.rammus, 3),
  shen: champion('쉔', championUrls.shen, 5),
  sona: champion('소나', championUrls.sona, 5),
  vex: champion('벡스', championUrls.vex, 3),
  viktor: champion('빅토르', championUrls.viktor, 4),
  xayah: champion('자야', championUrls.xayah, 5),
  zed: champion('제드', championUrls.zed, 5),
}

const GUIDE_TABS: {
  Icon: typeof BookOpen
  key: GuideTab
  label: string
  meta: string
}[] = [
  { Icon: Shield, key: 'traits', label: '시너지', meta: '설명 + 필요 챔피언' },
  { Icon: Package, key: 'items', label: '아이템', meta: '승률 + 조합 추천' },
  { Icon: Sparkles, key: 'augments', label: '증강체', meta: '티어표 + 보상표' },
  { Icon: Swords, key: 'champions', label: '챔피언', meta: '스탯 + 3신기' },
]

const BASE_TRAIT_GUIDES: TraitGuide[] = [
  {
    champions: [championRefs.jhin, championRefs.kaisa, championRefs.karma, championRefs.belveth, championRefs.pyke],
    count: 6,
    iconUrl: traitIconUrls.darkStar,
    levels: ['2', '4', '6', '8'],
    name: '암흑의 별',
    summary: '아군이 쓰러질수록 남은 암흑의 별 챔피언이 공격력과 주문력을 이어받는 후반 캐리형 시너지입니다.',
    tips: ['진 2성 이후 6암흑 전환', '앞라인은 2요새 또는 불한당으로 보강'],
    type: '후반 캐리',
  },
  {
    champions: [championRefs.vex, championRefs.illaoi, championRefs.poppy, championRefs.briar, championRefs.aurora],
    count: 4,
    iconUrl: traitIconUrls.vanguard,
    levels: ['2', '4', '6'],
    name: '선봉대',
    summary: '전열 유지력이 높고 전투가 길어질수록 딜러가 안정적으로 스킬을 굴릴 수 있는 기본 탱커 라인입니다.',
    tips: ['방어 아이템은 뽀삐보다 3코 이상 탱커에 집중', 'AP 캐리와 같이 쓰면 평균 등수가 안정적'],
    tone: 'silver',
    type: '전열',
  },
  {
    champions: [championRefs.corki, championRefs.vex, championRefs.aurora, championRefs.bard, championRefs.sona],
    count: 4,
    iconUrl: traitIconUrls.spirit,
    levels: ['2', '4', '6'],
    name: '정령족',
    summary: '스킬 순환과 회복을 동시에 챙기는 운영형 시너지로, 중반 체력 보존에 강합니다.',
    tips: ['쇼진, 블루 계열 아이템 효율이 높음', '4정령 이후 별돌보미나 운명술사로 확장'],
    type: '운영',
  },
  {
    champions: [championRefs.lulu, championRefs.nami, championRefs.xayah, championRefs.poppy],
    count: 4,
    iconUrl: traitIconUrls.stargazer,
    levels: ['2', '4', '6'],
    name: '별돌보미',
    summary: '후방 딜러 보호와 마나 보조에 집중된 시너지로, 3성 저코스트 캐리와 잘 맞습니다.',
    tips: ['룰루 3성 각이 보이면 리롤 가치 상승', '후반에는 자야 또는 소나로 화력 보강'],
    type: '보조',
  },
  {
    champions: [championRefs.masterYi, championRefs.pyke, championRefs.zed, championRefs.akali, championRefs.belveth],
    count: 4,
    iconUrl: traitIconUrls.rogue,
    levels: ['2', '4', '6'],
    name: '습격자',
    summary: '후방 진입과 폭발 피해가 강한 물리 캐리 시너지입니다. 상대 배치에 따른 가치 변동이 큽니다.',
    tips: ['최후의 속삭임 또는 무한의 대검 우선', '상대 메인 캐리 대각선 배치를 계속 확인'],
    tone: 'silver',
    type: '암살',
  },
  {
    champions: [championRefs.xayah, championRefs.kaisa, championRefs.akali, championRefs.jhin],
    count: 6,
    iconUrl: traitIconUrls.challenger,
    levels: ['2', '4', '6'],
    name: '도전자',
    summary: '공격 속도를 기반으로 전투 템포를 끌어올리는 시너지입니다. 아이템 완성도가 성능을 크게 좌우합니다.',
    tips: ['구인수, 라위 완성 후 캐리 선택', '탱커 2개 이상 없으면 후반 급락 가능'],
    type: 'AD 템포',
  },
  {
    champions: [championRefs.briar, championRefs.illaoi, championRefs.vex, championRefs.jinx, championRefs.aurora],
    count: 5,
    iconUrl: traitIconUrls.animaSquad,
    levels: ['3', '5', '7'],
    name: '동물특공대',
    summary: '처치 관여와 전투 지속력을 동시에 챙기는 중반 템포 시너지입니다. 완성 아이템이 빠르면 연승 유지가 쉽습니다.',
    tips: ['3동물로 중반 체력 보존', '전열 아이템과 AP 아이템을 같이 확보'],
    type: '템포',
  },
  {
    champions: [championRefs.karma, championRefs.lulu, championRefs.nami, championRefs.corki],
    count: 4,
    iconUrl: traitIconUrls.fateweaver,
    levels: ['2', '4', '6'],
    name: '운명술사',
    summary: '선택한 핵심 유닛에게 보너스를 집중시키는 유연한 시너지입니다. 아이템 방향을 늦게 확정하기 좋습니다.',
    tips: ['3-2 이후 캐리 방향 확정', '주문력 또는 마나 아이템과 궁합이 좋음'],
    type: '유연 운영',
  },
  {
    champions: [championRefs.ornn, championRefs.rammus, championRefs.blitzcrank, championRefs.shen, championRefs.poppy],
    count: 4,
    iconUrl: traitIconUrls.bastion,
    levels: ['2', '4', '6', '8'],
    name: '요새',
    summary: '방어력과 마법 저항력을 안정적으로 확보해 후방 캐리에게 시간을 벌어주는 전열 중심 시너지입니다.',
    tips: ['메인 탱커 2성 전까지 무리한 레벨업 금지', '후방 광역 딜러와 함께 쓰면 효율 상승'],
    tone: 'silver',
    type: '방어',
  },
  {
    champions: [championRefs.viktor, championRefs.corki, championRefs.masterYi, championRefs.belveth, championRefs.zed],
    count: 4,
    iconUrl: traitIconUrls.nova,
    levels: ['2', '4', '6'],
    name: 'N.O.V.A.',
    summary: '고밸류 유닛을 섞어 후반 전투력을 끌어올리는 확장형 시너지입니다. 8레벨 이후 전환 가치가 높습니다.',
    tips: ['4-2 이후 고코스트 상점 확인', '딜러가 겹치면 아이템 분배를 빠르게 결정'],
    type: '고밸류',
  },
]

const BASE_ITEM_STATS: ItemStatGuide[] = [
  {
    avgPlace: '3.52',
    bestUsers: [championRefs.jhin, championRefs.kaisa, championRefs.xayah],
    category: 'AD 치명타',
    combinations: [
      {
        items: [itemRefs.infinityEdge, itemRefs.lastWhisper, itemRefs.handOfJustice],
        label: '치명타 3신기',
        note: '방깎 + 흡혈까지 한 번에 확보',
      },
    ],
    imageUrl: itemUrls.infinityEdge,
    name: '무한의 대검',
    pickRate: '18.3%',
    top4: '71.2%',
    winRate: '62.1%',
  },
  {
    avgPlace: '3.38',
    bestUsers: [championRefs.ornn, championRefs.illaoi, championRefs.rammus],
    category: '탱커',
    combinations: [
      {
        items: [itemRefs.warmogsArmor, itemRefs.gargoyleStoneplate, itemRefs.dragonsClaw],
        label: '메인 탱커',
        note: '체력 + 저항력 균형이 가장 안정적',
      },
    ],
    imageUrl: itemUrls.warmogsArmor,
    name: '워모그의 갑옷',
    pickRate: '22.5%',
    top4: '74.3%',
    winRate: '64.8%',
  },
  {
    avgPlace: '3.29',
    bestUsers: [championRefs.aurelionSol, championRefs.vex, championRefs.viktor],
    category: 'AP 폭딜',
    combinations: [
      {
        items: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap],
        label: 'AP 3신기',
        note: '첫 스킬 폭발력을 최대로 끌어올림',
      },
    ],
    imageUrl: itemUrls.rabadonsDeathcap,
    name: '라바돈의 죽음모자',
    pickRate: '15.6%',
    top4: '76.4%',
    winRate: '66.2%',
  },
  {
    avgPlace: '3.61',
    bestUsers: [championRefs.sona, championRefs.karma, championRefs.bard],
    category: '마나',
    combinations: [
      {
        items: [itemRefs.spearOfShojin, itemRefs.archangelsStaff, itemRefs.morellonomicon],
        label: '장기전 캐스터',
        note: '스킬 횟수와 누적 주문력 확보',
      },
    ],
    imageUrl: itemUrls.spearOfShojin,
    name: '쇼진의 창',
    pickRate: '16.2%',
    top4: '68.5%',
    winRate: '59.4%',
  },
  {
    avgPlace: '3.46',
    bestUsers: [championRefs.masterYi, championRefs.zed, championRefs.akali],
    category: 'AD 지속딜',
    combinations: [
      {
        items: [itemRefs.guinsoosRageblade, itemRefs.infinityEdge, itemRefs.lastWhisper],
        label: '후열 캐리',
        note: '공속 스택과 방깎을 같이 사용',
      },
    ],
    imageUrl: itemUrls.guinsoosRageblade,
    name: '구인수의 격노검',
    pickRate: '14.4%',
    top4: '70.1%',
    winRate: '61.0%',
  },
  {
    avgPlace: '3.58',
    bestUsers: [championRefs.vex, championRefs.sona, championRefs.karma],
    category: '마나 순환',
    combinations: [
      {
        items: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.morellonomicon],
        label: '빠른 첫 스킬',
        note: '초반 스킬 타이밍을 앞당겨 전투를 여는 조합',
      },
    ],
    imageUrl: itemUrls.blueBuff,
    name: '푸른 파수꾼',
    pickRate: '13.8%',
    top4: '69.7%',
    winRate: '60.5%',
  },
  {
    avgPlace: '3.73',
    bestUsers: [championRefs.masterYi, championRefs.akali, championRefs.belveth],
    category: '근접 흡혈',
    combinations: [
      {
        items: [itemRefs.bloodthirster, itemRefs.titansResolve, itemRefs.handOfJustice],
        label: '근접 캐리 3신기',
        note: '보호막과 흡혈로 첫 진입 생존률을 높임',
      },
    ],
    imageUrl: itemUrls.bloodthirster,
    name: '피바라기',
    pickRate: '11.9%',
    top4: '66.4%',
    winRate: '57.9%',
  },
  {
    avgPlace: '3.42',
    bestUsers: [championRefs.aurelionSol, championRefs.viktor, championRefs.aurora],
    category: 'AP 치명타',
    combinations: [
      {
        items: [itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap, itemRefs.spearOfShojin],
        label: 'AP 폭발',
        note: '광역 스킬 캐리의 한 번 터지는 피해량을 극대화',
      },
    ],
    imageUrl: itemUrls.jeweledGauntlet,
    name: '보석 건틀릿',
    pickRate: '12.7%',
    top4: '72.8%',
    winRate: '63.0%',
  },
  {
    avgPlace: '3.50',
    bestUsers: [championRefs.ornn, championRefs.rammus, championRefs.blitzcrank],
    category: '방어 스택',
    combinations: [
      {
        items: [itemRefs.gargoyleStoneplate, itemRefs.warmogsArmor, itemRefs.dragonsClaw],
        label: '단독 전열',
        note: '집중 공격을 받는 메인 탱커에게 가장 안정적',
      },
    ],
    imageUrl: itemUrls.gargoyleStoneplate,
    name: '가고일 돌갑옷',
    pickRate: '15.2%',
    top4: '70.9%',
    winRate: '61.6%',
  },
  {
    avgPlace: '3.67',
    bestUsers: [championRefs.masterYi, championRefs.illaoi, championRefs.zed],
    category: '누적 전투',
    combinations: [
      {
        items: [itemRefs.titansResolve, itemRefs.bloodthirster, itemRefs.handOfJustice],
        label: '스택형 브루저',
        note: '전투가 길어질수록 공격과 방어를 같이 확보',
      },
    ],
    imageUrl: itemUrls.titansResolve,
    name: '거인의 결의',
    pickRate: '10.5%',
    top4: '67.1%',
    winRate: '58.2%',
  },
]

const BASE_AUGMENT_GUIDES: AugmentGuide[] = [
  {
    avgPlace: '3.21',
    description: '연패 후 경제력을 보존하면서 4-2 고밸류 전환을 노릴 수 있습니다.',
    name: '프리즘 티켓',
    pickRate: '9.8%',
    reward: '무료 상점 새로고침',
    tags: ['리롤', '경제'],
    tier: 'S',
    type: '프리즘',
    winRate: '67.4%',
  },
  {
    avgPlace: '3.37',
    description: '아이템 완성 방향을 늦게 확정해도 캐리 3신기를 맞추기 쉽습니다.',
    name: '판도라의 아이템',
    pickRate: '12.6%',
    reward: '라운드마다 대기석 아이템 변환',
    tags: ['아이템', '유연성'],
    tier: 'S',
    type: '골드',
    winRate: '65.1%',
  },
  {
    avgPlace: '3.64',
    description: '초반 전투력이 높고 연승 운영에서 골드 손실이 적습니다.',
    name: '사이버네틱 벌크',
    pickRate: '8.2%',
    reward: '아이템 보유 챔피언 체력 증가',
    tags: ['연승', '전열'],
    tier: 'A+',
    type: '골드',
    winRate: '60.8%',
  },
  {
    avgPlace: '3.82',
    description: '스킬 기반 덱의 첫 캐리 아이템 완성 전까지 안정적인 딜 보조가 가능합니다.',
    name: '마법 지팡이',
    pickRate: '7.1%',
    reward: '주문력 + 조합 아이템',
    tags: ['AP', '템포'],
    tier: 'A',
    type: '실버',
    winRate: '57.6%',
  },
  {
    avgPlace: '4.09',
    description: '특정 리롤 각이 없으면 보상이 늦게 열려 평균 순위가 흔들립니다.',
    name: '긴급한 재고',
    pickRate: '5.4%',
    reward: '상점 갱신 보상',
    tags: ['조건부', '리롤'],
    tier: 'B',
    type: '골드',
    winRate: '51.2%',
  },
  {
    avgPlace: '3.55',
    description: '초반 연승 중일 때 골드 손실 없이 전투력을 더 밀어붙일 수 있습니다.',
    name: '전투 훈련',
    pickRate: '6.9%',
    reward: '처치 관여 시 공격력 누적',
    tags: ['AD', '연승'],
    tier: 'A+',
    type: '골드',
    winRate: '61.4%',
  },
  {
    avgPlace: '3.76',
    description: '전열이 단단한 덱에서 후방 캐리가 스킬을 한 번 더 쓰게 만들어줍니다.',
    name: '명상',
    pickRate: '5.8%',
    reward: '아이템이 적은 유닛 마나 회복',
    tags: ['마나', '운영'],
    tier: 'A',
    type: '실버',
    winRate: '58.3%',
  },
  {
    avgPlace: '3.88',
    description: '시너지 문장이 필요한 고점 덱에서 4-2 전환 각을 넓혀줍니다.',
    name: '고대 기록 보관소',
    pickRate: '7.4%',
    reward: '문장 선택 보상',
    tags: ['문장', '전환'],
    tier: 'A',
    type: '골드',
    winRate: '56.9%',
  },
  {
    avgPlace: '4.18',
    description: '체력 손실이 큰 상황에서는 회복 타이밍이 늦어 하위권 위험이 있습니다.',
    name: '후반 전문가',
    pickRate: '4.7%',
    reward: '9레벨 도달 보상',
    tags: ['후반', '고위험'],
    tier: 'B',
    type: '골드',
    winRate: '50.4%',
  },
  {
    avgPlace: '3.31',
    description: '아이템과 골드를 동시에 받아 초반 방향성을 빠르게 확정할 수 있습니다.',
    name: '찬란한 유물',
    pickRate: '6.2%',
    reward: '찬란한 아이템 선택',
    tags: ['아이템', '고점'],
    tier: 'S',
    type: '프리즘',
    winRate: '66.8%',
  },
]

const REWARD_ROWS: RewardRow[] = [
  { condition: '2-1 전투 증강', reward: '초반 전투력 또는 경제 증강 우선', stage: '2-1' },
  { condition: '3-2 방향 확정', reward: '캐리 아이템, 시너지 문장, 리롤 보상 체크', stage: '3-2' },
  { condition: '4-2 마무리', reward: '고밸류 전환, 전설 챔피언, 전투 증강 선택', stage: '4-2' },
  { condition: '프리즘 보상', reward: '아이템 완성권, 대량 골드, 고코스트 보강', stage: '전 구간' },
]

const AUGMENT_PLANS: AugmentPlan[] = [
  {
    key: 'fast8',
    label: 'Fast 8',
    stages: [
      { choice: '사이버네틱 벌크', focus: '연승 체력 보존', stage: '2-1' },
      { choice: '판도라의 아이템', focus: '캐리 아이템 확정', stage: '3-2' },
      { choice: '레벨 업 보상', focus: '4코스트 캐리 전환', stage: '4-2' },
    ],
  },
  {
    key: 'reroll',
    label: '리롤',
    stages: [
      { choice: '프리즘 티켓', focus: '상점 갱신 절약', stage: '2-1' },
      { choice: '긴급한 재고', focus: '3성 각 확인', stage: '3-2' },
      { choice: '전투 증강', focus: '캐리 완성 후 순방', stage: '4-2' },
    ],
  },
  {
    key: 'flex',
    label: '유연 운영',
    stages: [
      { choice: '아이템 가방', focus: '조합 방향 보류', stage: '2-1' },
      { choice: '판도라의 아이템', focus: 'AP/AD 선택', stage: '3-2' },
      { choice: '시너지 문장', focus: '상위 덱 전환', stage: '4-2' },
    ],
  },
]

const BASE_CHAMPION_GUIDES: ChampionGuide[] = [
  {
    bestItems: [itemRefs.infinityEdge, itemRefs.lastWhisper, itemRefs.handOfJustice],
    cost: 5,
    imageUrl: championUrls.jhin,
    name: '진',
    position: '후열 중앙',
    role: 'AD 캐리',
    stats: { ad: 84, armor: 35, attackSpeed: '0.80', hp: 900, mana: '0/50', mr: 35, range: 4 },
    traits: ['암흑의 별', '저격수'],
  },
  {
    bestItems: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap],
    cost: 3,
    imageUrl: championUrls.vex,
    name: '벡스',
    position: '후열 2열',
    role: 'AP 캐리',
    stats: { ad: 45, armor: 30, attackSpeed: '0.75', hp: 720, mana: '20/70', mr: 30, range: 4 },
    traits: ['선봉대', '정령족'],
  },
  {
    bestItems: [itemRefs.warmogsArmor, itemRefs.gargoyleStoneplate, itemRefs.dragonsClaw],
    cost: 4,
    imageUrl: championUrls.ornn,
    name: '오른',
    position: '전열 중앙',
    role: '메인 탱커',
    stats: { ad: 60, armor: 60, attackSpeed: '0.60', hp: 1100, mana: '50/120', mr: 60, range: 1 },
    traits: ['요새', '선봉대'],
  },
  {
    bestItems: [itemRefs.guinsoosRageblade, itemRefs.infinityEdge, itemRefs.lastWhisper],
    cost: 5,
    imageUrl: championUrls.xayah,
    name: '자야',
    position: '후열 모서리',
    role: 'AD 지속딜',
    stats: { ad: 76, armor: 35, attackSpeed: '0.85', hp: 880, mana: '30/90', mr: 35, range: 4 },
    traits: ['도전자', '별돌보미'],
  },
  {
    bestItems: [itemRefs.spearOfShojin, itemRefs.archangelsStaff, itemRefs.morellonomicon],
    cost: 5,
    imageUrl: championUrls.sona,
    name: '소나',
    position: '후열 중앙',
    role: '서포트 캐리',
    stats: { ad: 40, armor: 30, attackSpeed: '0.75', hp: 820, mana: '40/100', mr: 30, range: 4 },
    traits: ['정령족', '우주 그루브'],
  },
  {
    bestItems: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap],
    cost: 4,
    imageUrl: championUrls.aurelionSol,
    name: '아우렐리온 솔',
    position: '후열 모서리',
    role: 'AP 광역딜',
    stats: { ad: 50, armor: 35, attackSpeed: '0.70', hp: 850, mana: '30/90', mr: 35, range: 4 },
    traits: ['요새', '정령족'],
  },
  {
    bestItems: [itemRefs.bloodthirster, itemRefs.titansResolve, itemRefs.handOfJustice],
    cost: 4,
    imageUrl: championUrls.masterYi,
    name: '마스터 이',
    position: '2열 측면',
    role: '근접 캐리',
    stats: { ad: 72, armor: 45, attackSpeed: '0.90', hp: 950, mana: '20/80', mr: 45, range: 1 },
    traits: ['습격자', '도전자'],
  },
  {
    bestItems: [itemRefs.spearOfShojin, itemRefs.morellonomicon, itemRefs.adaptiveHelm],
    cost: 4,
    imageUrl: championUrls.karma,
    name: '카르마',
    position: '후열 2열',
    role: 'AP 유틸',
    stats: { ad: 44, armor: 30, attackSpeed: '0.75', hp: 760, mana: '20/80', mr: 30, range: 4 },
    traits: ['암흑의 별', '운명술사'],
  },
  {
    bestItems: [itemRefs.warmogsArmor, itemRefs.gargoyleStoneplate, itemRefs.dragonsClaw],
    cost: 3,
    imageUrl: championUrls.illaoi,
    name: '일라오이',
    position: '전열 측면',
    role: '서브 탱커',
    stats: { ad: 58, armor: 50, attackSpeed: '0.60', hp: 950, mana: '40/100', mr: 50, range: 1 },
    traits: ['선봉대', '동물특공대'],
  },
  {
    bestItems: [itemRefs.infinityEdge, itemRefs.handOfJustice, itemRefs.bloodthirster],
    cost: 2,
    imageUrl: championUrls.akali,
    name: '아칼리',
    position: '2열 측면',
    role: '암살 캐리',
    stats: { ad: 57, armor: 35, attackSpeed: '0.80', hp: 700, mana: '0/60', mr: 35, range: 1 },
    traits: ['습격자', '도전자'],
  },
  {
    bestItems: [itemRefs.guinsoosRageblade, itemRefs.infinityEdge, itemRefs.lastWhisper],
    cost: 3,
    imageUrl: championUrls.kaisa,
    name: '카이사',
    position: '후열 모서리',
    role: 'AD 캐리',
    stats: { ad: 66, armor: 30, attackSpeed: '0.85', hp: 760, mana: '20/80', mr: 30, range: 4 },
    traits: ['암흑의 별', '도전자'],
  },
  {
    bestItems: [itemRefs.spearOfShojin, itemRefs.morellonomicon, itemRefs.adaptiveHelm],
    cost: 4,
    imageUrl: championUrls.nami,
    name: '나미',
    position: '후열 중앙',
    role: 'AP 서포트',
    stats: { ad: 42, armor: 30, attackSpeed: '0.70', hp: 780, mana: '30/90', mr: 30, range: 4 },
    traits: ['정령족', '별돌보미'],
  },
  {
    bestItems: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap],
    cost: 3,
    imageUrl: championUrls.lulu,
    name: '룰루',
    position: '후열 2열',
    role: 'AP 리롤',
    stats: { ad: 40, armor: 25, attackSpeed: '0.75', hp: 680, mana: '20/70', mr: 25, range: 4 },
    traits: ['별돌보미', '운명술사'],
  },
  {
    bestItems: [itemRefs.infinityEdge, itemRefs.handOfJustice, itemRefs.bloodthirster],
    cost: 2,
    imageUrl: championUrls.pyke,
    name: '파이크',
    position: '2열 측면',
    role: '암살 유틸',
    stats: { ad: 58, armor: 35, attackSpeed: '0.80', hp: 720, mana: '0/70', mr: 35, range: 1 },
    traits: ['습격자', '암흑의 별'],
  },
  {
    bestItems: [itemRefs.warmogsArmor, itemRefs.gargoyleStoneplate, itemRefs.dragonsClaw],
    cost: 3,
    imageUrl: championUrls.rammus,
    name: '람머스',
    position: '전열 중앙',
    role: '메인 탱커',
    stats: { ad: 50, armor: 65, attackSpeed: '0.55', hp: 980, mana: '40/100', mr: 45, range: 1 },
    traits: ['요새', '선봉대'],
  },
  {
    bestItems: [itemRefs.infinityEdge, itemRefs.lastWhisper, itemRefs.handOfJustice],
    cost: 5,
    imageUrl: championUrls.zed,
    name: '제드',
    position: '2열 측면',
    role: 'AD 암살',
    stats: { ad: 82, armor: 40, attackSpeed: '0.90', hp: 920, mana: '0/60', mr: 40, range: 1 },
    traits: ['습격자', 'N.O.V.A.'],
  },
  {
    bestItems: [itemRefs.spearOfShojin, itemRefs.archangelsStaff, itemRefs.morellonomicon],
    cost: 5,
    imageUrl: championUrls.bard,
    name: '바드',
    position: '후열 중앙',
    role: 'AP 보조',
    stats: { ad: 44, armor: 35, attackSpeed: '0.75', hp: 860, mana: '40/100', mr: 35, range: 4 },
    traits: ['정령족', '운명술사'],
  },
  {
    bestItems: [itemRefs.blueBuff, itemRefs.jeweledGauntlet, itemRefs.rabadonsDeathcap],
    cost: 4,
    imageUrl: championUrls.viktor,
    name: '빅토르',
    position: '후열 모서리',
    role: 'AP 광역딜',
    stats: { ad: 48, armor: 30, attackSpeed: '0.70', hp: 820, mana: '30/90', mr: 30, range: 4 },
    traits: ['N.O.V.A.', '초능력'],
  },
  {
    bestItems: [itemRefs.bloodthirster, itemRefs.titansResolve, itemRefs.handOfJustice],
    cost: 1,
    imageUrl: championUrls.briar,
    name: '브라이어',
    position: '전열 측면',
    role: '초반 브루저',
    stats: { ad: 54, armor: 35, attackSpeed: '0.75', hp: 650, mana: '0/60', mr: 35, range: 1 },
    traits: ['동물특공대', '선봉대'],
  },
  {
    bestItems: [itemRefs.warmogsArmor, itemRefs.gargoyleStoneplate, itemRefs.dragonsClaw],
    cost: 1,
    imageUrl: championUrls.poppy,
    name: '뽀삐',
    position: '전열 중앙',
    role: '초반 탱커',
    stats: { ad: 48, armor: 45, attackSpeed: '0.60', hp: 700, mana: '40/90', mr: 45, range: 1 },
    traits: ['선봉대', '별돌보미'],
  },
]

function expandGuideSamples<T extends { name: string }>(
  samples: T[],
  pageSize: number,
  formatName: (name: string, variant: string, copyIndex: number) => string,
) {
  const targetCount = pageSize * SAMPLE_PAGE_COUNT
  if (samples.length >= targetCount) return samples.slice(0, targetCount)

  return Array.from({ length: targetCount }, (_, index) => {
    if (index < samples.length) return samples[index]

    const copyIndex = index - samples.length
    const source = samples[copyIndex % samples.length]
    const variant = SAMPLE_VARIANTS[Math.floor(copyIndex / samples.length) % SAMPLE_VARIANTS.length]

    return {
      ...source,
      name: formatName(source.name, variant, copyIndex + 1),
    }
  })
}

const TRAIT_GUIDES = expandGuideSamples(
  BASE_TRAIT_GUIDES,
  TRAIT_PAGE_SIZE,
  (name, variant, copyIndex) => `${name} ${variant} ${copyIndex}`,
)

const ITEM_STATS = expandGuideSamples(
  BASE_ITEM_STATS,
  DEFAULT_GUIDE_PAGE_SIZE,
  (name, variant, copyIndex) => `${name} ${variant} 빌드 ${copyIndex}`,
)

const AUGMENT_GUIDES = expandGuideSamples(
  BASE_AUGMENT_GUIDES,
  DEFAULT_GUIDE_PAGE_SIZE,
  (name, variant, copyIndex) => `${name} ${variant} ${copyIndex}`,
)

const CHAMPION_GUIDES = expandGuideSamples(
  BASE_CHAMPION_GUIDES,
  CHAMPION_PAGE_SIZE,
  (name, variant, copyIndex) => `${name} ${variant} 빌드 ${copyIndex}`,
)

function normalizeText(value: string) {
  return value.toLowerCase().replace(/\s/g, '')
}

function matchesSearch(query: string, fields: string[]) {
  const normalizedQuery = normalizeText(query.trim())
  if (!normalizedQuery) return true

  return fields.some((field) => normalizeText(field).includes(normalizedQuery))
}

function parseMetric(value: string) {
  return Number(value.replace(/[^\d.-]/g, '')) || 0
}

function sortByMetric<T extends SortableMetricItem>(items: T[], sortKey: MetricSortKey, sortDir: SortDir) {
  return [...items].sort((a, b) => {
    const first = parseMetric(sortKey === 'top4' ? a.top4 ?? '0' : a[sortKey])
    const second = parseMetric(sortKey === 'top4' ? b.top4 ?? '0' : b[sortKey])
    const base = first < second ? -1 : first > second ? 1 : 0

    return sortDir === 'asc' ? base : -base
  })
}

function getTotalPages(totalItems: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

function getPageItems<T>(items: T[], page: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  const startIndex = (page - 1) * pageSize
  return items.slice(startIndex, startIndex + pageSize)
}

function EmptyState() {
  return (
    <div className={styles.emptyState}>
      <Search size={18} />
      <span>검색 결과가 없습니다.</span>
    </div>
  )
}

function SortHeaderButton({
  active,
  direction,
  label,
  onClick,
}: {
  active: boolean
  direction: SortDir
  label: string
  onClick: () => void
}) {
  return (
    <button className={styles.sortButton} onClick={onClick} type="button">
      {label}
      <span>{active ? (direction === 'asc' ? '▲' : '▼') : '↕'}</span>
    </button>
  )
}

function Pagination({
  currentPage,
  onPageChange,
  totalPages,
}: {
  currentPage: number
  onPageChange: (page: number) => void
  totalPages: number
}) {
  if (totalPages <= 1) return null

  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)
  const pages = Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)

  return (
    <nav className={styles.pagination} aria-label="가이드 페이지">
      <button
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        <ChevronLeft size={15} />
        이전
      </button>
      {windowStart > 1 && <span className={styles.pageEllipsis}>...</span>}
      {pages.map((page) => (
        <button
          aria-current={currentPage === page ? 'page' : undefined}
          className={currentPage === page ? styles.activePage : ''}
          key={page}
          onClick={() => onPageChange(page)}
          type="button"
        >
          {page}
        </button>
      ))}
      {windowEnd < totalPages && <span className={styles.pageEllipsis}>...</span>}
      <button
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        다음
        <ChevronRight size={15} />
      </button>
    </nav>
  )
}

function StatBadge({ label, value }: { label: string; value: string }) {
  return (
    <span className={styles.statBadge}>
      <small>{label}</small>
      <strong>{value}</strong>
    </span>
  )
}

function LinkedChampionMini({
  champion,
  onSelect,
}: {
  champion: ChampionRef
  onSelect: (championName: string) => void
}) {
  return (
    <button
      className={styles.championMini}
      onClick={() => onSelect(champion.name)}
      title={`${champion.name} 챔피언 보기`}
      type="button"
    >
      <img src={champion.imageUrl} alt={champion.name} />
      <span>{champion.name}</span>
      <b>{champion.cost}</b>
    </button>
  )
}

function ItemIconStrip({
  items,
  onItemSelect,
}: {
  items: ItemRef[]
  onItemSelect?: (itemName: string) => void
}) {
  return (
    <span className={styles.itemIconStrip}>
      {items.map((itemRef) => (
        onItemSelect ? (
          <button
            className={styles.itemIconButton}
            key={itemRef.name}
            onClick={(event) => {
              event.stopPropagation()
              onItemSelect(itemRef.name)
            }}
            title={`${itemRef.name} 아이템 보기`}
            type="button"
          >
            <img src={itemRef.imageUrl} alt={itemRef.name} />
          </button>
        ) : (
          <img src={itemRef.imageUrl} alt={itemRef.name} title={itemRef.name} key={itemRef.name} />
        )
      ))}
    </span>
  )
}

function GuideQuickAccess({
  favoriteChampions,
  onJump,
  recentGuides,
}: {
  favoriteChampions: string[]
  onJump: (tab: GuideTab, query: string, label?: string) => void
  recentGuides: RecentGuide[]
}) {
  if (favoriteChampions.length === 0 && recentGuides.length === 0) return null

  return (
    <section className={styles.quickAccess} aria-label="빠른 이동">
      {favoriteChampions.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>즐겨찾기</strong>
          {favoriteChampions.slice(0, 6).map((name) => (
            <button key={name} onClick={() => onJump('champions', name, name)} type="button">
              {name}
            </button>
          ))}
        </div>
      )}
      {recentGuides.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>최근 본 가이드</strong>
          {recentGuides.slice(0, 6).map((guide) => (
            <button
              key={`${guide.tab}-${guide.query}`}
              onClick={() => onJump(guide.tab, guide.query, guide.label)}
              type="button"
            >
              {guide.label}
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

function ChampionDetailDialog({
  champion,
  isFavorite,
  onClose,
  onFavoriteToggle,
  onItemSelect,
}: {
  champion: ChampionGuide
  isFavorite: boolean
  onClose: () => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
}) {
  return (
    <div className={styles.dialogBackdrop} role="presentation" onClick={onClose}>
      <section
        aria-label={`${champion.name} 상세 정보`}
        className={styles.championDialog}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <button className={styles.dialogCloseButton} onClick={onClose} type="button">
          <X size={17} />
        </button>
        <div className={styles.dialogHero}>
          <img src={champion.imageUrl} alt={champion.name} />
          <div>
            <strong>{champion.name}</strong>
            <span>{champion.role}</span>
            <p>{champion.traits.join(' / ')}</p>
          </div>
        </div>
        <div className={styles.dialogItems}>
          <b>3신기</b>
          <ItemIconStrip items={champion.bestItems} onItemSelect={onItemSelect} />
        </div>
        <dl className={styles.dialogStats}>
          <div><dt>체력</dt><dd>{champion.stats.hp}</dd></div>
          <div><dt>공격력</dt><dd>{champion.stats.ad}</dd></div>
          <div><dt>공속</dt><dd>{champion.stats.attackSpeed}</dd></div>
          <div><dt>마나</dt><dd>{champion.stats.mana}</dd></div>
          <div><dt>방어</dt><dd>{champion.stats.armor}</dd></div>
          <div><dt>마저</dt><dd>{champion.stats.mr}</dd></div>
        </dl>
        <p className={styles.dialogPosition}>권장 배치: {champion.position}</p>
        <button
          className={`${styles.dialogFavoriteButton} ${isFavorite ? styles.favoriteActive : ''}`}
          onClick={() => onFavoriteToggle(champion.name)}
          type="button"
        >
          <Star size={15} />
          {isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
        </button>
      </section>
    </div>
  )
}

function TraitGuideView({
  onChampionSelect,
  query,
}: {
  onChampionSelect: (championName: string) => void
  query: string
}) {
  const [currentPage, setCurrentPage] = useState(1)
  const traits = TRAIT_GUIDES.filter((traitGuide) =>
    matchesSearch(query, [
      traitGuide.name,
      traitGuide.summary,
      traitGuide.type,
      ...traitGuide.champions.map((championRef) => championRef.name),
    ]),
  )
  const totalPages = getTotalPages(traits.length, TRAIT_PAGE_SIZE)
  const safePage = Math.min(currentPage, totalPages)
  const visibleTraits = getPageItems(traits, safePage, TRAIT_PAGE_SIZE)

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages)
  }, [currentPage, totalPages])

  return (
    <>
      <section className={styles.traitGrid}>
        {traits.length === 0 && <EmptyState />}
        {visibleTraits.map((traitGuide) => (
          <article className={styles.traitCard} key={traitGuide.name}>
            <div className={styles.traitTop}>
              <TraitHexBadge
                count={traitGuide.count}
                iconUrl={traitGuide.iconUrl}
                name={traitGuide.name}
                tone={traitGuide.tone}
              />
              <div className={styles.traitTitle}>
                <h2>{traitGuide.name}</h2>
                <span>{traitGuide.type}</span>
              </div>
              <div className={styles.levelTrack}>
                {traitGuide.levels.map((level) => (
                  <b className={level === String(traitGuide.count) ? styles.levelActive : ''} key={level}>
                    {level}
                  </b>
                ))}
              </div>
            </div>
            <p>{traitGuide.summary}</p>
            <div className={styles.championLine}>
              {traitGuide.champions.map((championRef) => (
                <LinkedChampionMini champion={championRef} key={championRef.name} onSelect={onChampionSelect} />
              ))}
            </div>
            <div className={styles.tipLine}>
              {traitGuide.tips.map((tip) => (
                <span key={tip}>{tip}</span>
              ))}
            </div>
          </article>
        ))}
      </section>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={totalPages} />
    </>
  )
}

function ItemStatsView({
  onChampionSelect,
  query,
}: {
  onChampionSelect: (championName: string) => void
  query: string
}) {
  const [currentPage, setCurrentPage] = useState(1)
  const [sortDir, setSortDir] = useState<SortDir>('desc')
  const [sortKey, setSortKey] = useState<MetricSortKey>('winRate')
  const items = ITEM_STATS.filter((itemStat) =>
    matchesSearch(query, [
      itemStat.name,
      itemStat.category,
      ...itemStat.bestUsers.map((championRef) => championRef.name),
      ...itemStat.combinations.map((combination) => combination.label),
    ]),
  )
  const sortedItems = sortByMetric(items, sortKey, sortDir)
  const totalPages = getTotalPages(sortedItems.length)
  const safePage = Math.min(currentPage, totalPages)
  const visibleItems = getPageItems(sortedItems, safePage)

  function handleSort(nextSortKey: MetricSortKey) {
    if (sortKey === nextSortKey) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(nextSortKey)
      setSortDir(nextSortKey === 'avgPlace' ? 'asc' : 'desc')
    }
    setCurrentPage(1)
  }

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages)
  }, [currentPage, totalPages])

  return (
    <>
      <div className={styles.tableWrap}>
        <table className={styles.itemTable}>
          <thead>
            <tr>
              <th className={styles.nameCol}>아이템</th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'winRate'}
                  direction={sortDir}
                  label="승률"
                  onClick={() => handleSort('winRate')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'top4'}
                  direction={sortDir}
                  label="TOP4"
                  onClick={() => handleSort('top4')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'avgPlace'}
                  direction={sortDir}
                  label="평균 등수"
                  onClick={() => handleSort('avgPlace')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'pickRate'}
                  direction={sortDir}
                  label="픽률"
                  onClick={() => handleSort('pickRate')}
                />
              </th>
              <th className={styles.userCol}>추천 챔피언</th>
              <th className={styles.comboCol}>조합 추천</th>
            </tr>
          </thead>
          <tbody>
            {visibleItems.map((itemStat) => (
              <tr key={itemStat.name}>
                <td className={styles.itemNameCell}>
                  <img src={itemStat.imageUrl} alt={itemStat.name} />
                  <div>
                    <strong>{itemStat.name}</strong>
                    <span>{itemStat.category}</span>
                  </div>
                </td>
                <td className={styles.winRate}>{itemStat.winRate}</td>
                <td className={styles.top4}>{itemStat.top4}</td>
                <td className={styles.avgPlace}>#{itemStat.avgPlace}</td>
                <td className={styles.pickRate}>{itemStat.pickRate}</td>
                <td>
                  <div className={styles.avatarStack}>
                    {itemStat.bestUsers.map((championRef) => (
                      <button
                        className={styles.avatarButton}
                        key={championRef.name}
                        onClick={() => onChampionSelect(championRef.name)}
                        title={`${championRef.name} 챔피언 보기`}
                        type="button"
                      >
                        <img src={championRef.imageUrl} alt={championRef.name} />
                      </button>
                    ))}
                  </div>
                </td>
                <td>
                  {itemStat.combinations.map((combination) => (
                    <div className={styles.comboCell} key={combination.label}>
                      <ItemIconStrip items={combination.items} />
                      <div>
                        <strong>{combination.label}</strong>
                        <span>{combination.note}</span>
                      </div>
                    </div>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && <EmptyState />}
      </div>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={totalPages} />

      <section className={styles.metricCards}>
        <article>
          <Rows3 size={18} />
          <strong>매치 기반 집계</strong>
          <span>matchId별 최종 배치와 장착 아이템을 묶어 승률, TOP4, 평균 등수를 계산합니다.</span>
        </article>
        <article>
          <Gem size={18} />
          <strong>3신기 우선순위</strong>
          <span>완성 아이템 3개 조합을 캐리 챔피언별로 비교할 수 있게 확장합니다.</span>
        </article>
        <article>
          <Trophy size={18} />
          <strong>표본 필터</strong>
          <span>마스터+와 전체 랭크를 분리해 메타 왜곡을 줄이는 구성이 좋습니다.</span>
        </article>
      </section>
    </>
  )
}

function AugmentGuideView({ query }: { query: string }) {
  const [planKey, setPlanKey] = useState<AugmentPlanKey>('fast8')
  const [currentPage, setCurrentPage] = useState(1)
  const [sortDir, setSortDir] = useState<SortDir>('desc')
  const [sortKey, setSortKey] = useState<MetricSortKey>('winRate')
  const augments = AUGMENT_GUIDES.filter((augment) =>
    matchesSearch(query, [augment.name, augment.description, augment.reward, augment.type, ...augment.tags]),
  )
  const sortedAugments = sortByMetric(augments, sortKey, sortDir)
  const totalPages = getTotalPages(sortedAugments.length)
  const safePage = Math.min(currentPage, totalPages)
  const visibleAugments = getPageItems(sortedAugments, safePage)
  const selectedPlan = AUGMENT_PLANS.find((plan) => plan.key === planKey) ?? AUGMENT_PLANS[0]

  function handleSort(nextSortKey: MetricSortKey) {
    if (sortKey === nextSortKey) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(nextSortKey)
      setSortDir(nextSortKey === 'avgPlace' ? 'asc' : 'desc')
    }
    setCurrentPage(1)
  }

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages)
  }, [currentPage, totalPages])

  return (
    <>
      <div className={styles.augmentLayout}>
        <section className={styles.tableWrap}>
          <table className={styles.augmentTable}>
            <thead>
              <tr>
                <th>티어</th>
                <th className={styles.nameCol}>증강체</th>
                <th>종류</th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'winRate'}
                    direction={sortDir}
                    label="승률"
                    onClick={() => handleSort('winRate')}
                  />
                </th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'avgPlace'}
                    direction={sortDir}
                    label="평균 등수"
                    onClick={() => handleSort('avgPlace')}
                  />
                </th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'pickRate'}
                    direction={sortDir}
                    label="픽률"
                    onClick={() => handleSort('pickRate')}
                  />
                </th>
                <th className={styles.rewardCol}>보상</th>
              </tr>
            </thead>
            <tbody>
              {visibleAugments.map((augment) => (
                <tr key={augment.name}>
                  <td><TierBadge value={augment.tier} /></td>
                  <td className={styles.augmentNameCell}>
                    <strong>{augment.name}</strong>
                    <span>{augment.description}</span>
                    <div>
                      {augment.tags.map((tag) => <b key={tag}>{tag}</b>)}
                    </div>
                  </td>
                  <td>{augment.type}</td>
                  <td className={styles.winRate}>{augment.winRate}</td>
                  <td className={styles.avgPlace}>#{augment.avgPlace}</td>
                  <td className={styles.pickRate}>{augment.pickRate}</td>
                  <td className={styles.rewardCell}>{augment.reward}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {augments.length === 0 && <EmptyState />}
        </section>

        <aside className={styles.rewardPanel}>
          <div className={styles.panelHeading}>
            <Trophy size={17} />
            <h2>보상표</h2>
          </div>
          <div className={styles.rewardList}>
            {REWARD_ROWS.map((row) => (
              <div className={styles.rewardRow} key={`${row.stage}-${row.condition}`}>
                <b>{row.stage}</b>
                <strong>{row.condition}</strong>
                <span>{row.reward}</span>
              </div>
            ))}
          </div>
        </aside>
      </div>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={totalPages} />

      <section className={styles.plannerPanel}>
        <div className={styles.plannerTop}>
          <div>
            <span className={styles.sectionBadge}>배치툴</span>
            <h2>증강 선택 플랜</h2>
          </div>
          <div className={styles.planTabs}>
            {AUGMENT_PLANS.map((plan) => (
              <button
                className={plan.key === planKey ? styles.planActive : ''}
                key={plan.key}
                onClick={() => setPlanKey(plan.key)}
                type="button"
              >
                {plan.label}
              </button>
            ))}
          </div>
        </div>

        <div className={styles.plannerBody}>
          <div className={styles.stageCards}>
            {selectedPlan.stages.map((stage) => (
              <article className={styles.stageCard} key={`${selectedPlan.key}-${stage.stage}`}>
                <span>{stage.stage}</span>
                <strong>{stage.choice}</strong>
                <p>{stage.focus}</p>
              </article>
            ))}
          </div>
          <div className={styles.boardTool} aria-label="증강 선택 이후 배치 미리보기">
            {Array.from({ length: 21 }).map((_, index) => (
              <span
                className={
                  index === 2 || index === 4 || index === 10 || index === 16
                    ? styles.boardCellActive
                    : ''
                }
                key={index}
              />
            ))}
          </div>
        </div>
      </section>
    </>
  )
}

function ChampionGuideView({
  favoriteChampions,
  onChampionOpen,
  onFavoriteToggle,
  onItemSelect,
  query,
}: {
  favoriteChampions: string[]
  onChampionOpen: (championName: string) => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
  query: string
}) {
  const [costFilter, setCostFilter] = useState<ChampionCostFilter>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const [selectedChampion, setSelectedChampion] = useState<ChampionGuide | null>(null)
  const champions = CHAMPION_GUIDES.filter((championGuide) => {
    const matchesCost = costFilter === 'all' || championGuide.cost === costFilter
    const matchesQuery = matchesSearch(query, [
      championGuide.name,
      championGuide.role,
      championGuide.position,
      ...championGuide.traits,
      ...championGuide.bestItems.map((itemRef) => itemRef.name),
    ])

    return matchesCost && matchesQuery
  })
  const totalPages = getTotalPages(champions.length, CHAMPION_PAGE_SIZE)
  const safePage = Math.min(currentPage, totalPages)
  const visibleChampions = getPageItems(champions, safePage, CHAMPION_PAGE_SIZE)

  useEffect(() => {
    setCurrentPage(1)
  }, [costFilter, query])

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages)
  }, [currentPage, totalPages])

  return (
    <>
      <div className={styles.costFilter} aria-label="챔피언 비용 필터">
        {(['all', 1, 2, 3, 4, 5] as const).map((cost) => (
          <button
            className={costFilter === cost ? styles.costActive : ''}
            key={cost}
            onClick={() => setCostFilter(cost)}
            type="button"
          >
            {cost === 'all' ? '전체' : `${cost}코스트`}
          </button>
        ))}
      </div>
      <section className={styles.championGrid}>
        {champions.length === 0 && <EmptyState />}
        {visibleChampions.map((championGuide) => (
          <article
            className={styles.championCard}
            key={championGuide.name}
            onClick={() => {
              setSelectedChampion(championGuide)
              onChampionOpen(championGuide.name)
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                setSelectedChampion(championGuide)
                onChampionOpen(championGuide.name)
              }
            }}
            tabIndex={0}
          >
            <button
              className={`${styles.favoriteButton} ${favoriteChampions.includes(championGuide.name) ? styles.favoriteActive : ''}`}
              onClick={(event) => {
                event.stopPropagation()
                onFavoriteToggle(championGuide.name)
              }}
              title={favoriteChampions.includes(championGuide.name) ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              type="button"
            >
              <Star size={14} />
            </button>
            <div className={styles.championPortrait}>
              <img src={championGuide.imageUrl} alt={championGuide.name} />
              <span>{championGuide.cost}</span>
            </div>
            <div className={styles.championInfo}>
              <strong>{championGuide.name}</strong>
              <span>{championGuide.role}</span>
            </div>
            <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
            <div className={styles.championTooltip} role="tooltip">
              <div className={styles.tooltipTop}>
                <img src={championGuide.imageUrl} alt="" />
                <div>
                  <strong>{championGuide.name}</strong>
                  <span>{championGuide.traits.join(' / ')}</span>
                </div>
              </div>
              <div className={styles.tooltipItems}>
                <b>3신기</b>
                <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
              </div>
              <dl className={styles.statGrid}>
                <div><dt>체력</dt><dd>{championGuide.stats.hp}</dd></div>
                <div><dt>공격력</dt><dd>{championGuide.stats.ad}</dd></div>
                <div><dt>공속</dt><dd>{championGuide.stats.attackSpeed}</dd></div>
                <div><dt>마나</dt><dd>{championGuide.stats.mana}</dd></div>
                <div><dt>방어</dt><dd>{championGuide.stats.armor}</dd></div>
                <div><dt>마저</dt><dd>{championGuide.stats.mr}</dd></div>
              </dl>
              <p>권장 배치: {championGuide.position}</p>
            </div>
          </article>
        ))}
      </section>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={totalPages} />
      {selectedChampion && (
        <ChampionDetailDialog
          champion={selectedChampion}
          isFavorite={favoriteChampions.includes(selectedChampion.name)}
          onClose={() => setSelectedChampion(null)}
          onFavoriteToggle={onFavoriteToggle}
          onItemSelect={(itemName) => {
            setSelectedChampion(null)
            onItemSelect(itemName)
          }}
        />
      )}
    </>
  )
}

function Guide() {
  const [activeTab, setActiveTab] = useState<GuideTab>('traits')
  const [favoriteChampions, setFavoriteChampions] = useState<string[]>([])
  const [recentGuides, setRecentGuides] = useState<RecentGuide[]>([])
  const [search, setSearch] = useState('')

  const activeTabInfo = useMemo(
    () => GUIDE_TABS.find((tab) => tab.key === activeTab) ?? GUIDE_TABS[0],
    [activeTab],
  )

  function addRecentGuide(guide: RecentGuide) {
    setRecentGuides((current) => [
      guide,
      ...current.filter((item) => item.query !== guide.query || item.tab !== guide.tab),
    ].slice(0, 6))
  }

  function jumpToGuide(tab: GuideTab, query: string, label = query) {
    setActiveTab(tab)
    setSearch(query)
    addRecentGuide({ label, query, tab })
  }

  function handleFavoriteToggle(championName: string) {
    setFavoriteChampions((current) => (
      current.includes(championName)
        ? current.filter((name) => name !== championName)
        : [championName, ...current].slice(0, 12)
    ))
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <span className={styles.kicker}>
              <BookOpen size={15} />
              SET 17 GUIDE
            </span>
            <h1>게임 가이드</h1>
            <p>시너지, 아이템, 증강체, 챔피언 정보를 한 화면에서 빠르게 비교합니다.</p>
          </div>
          <div className={styles.headerStats}>
            <StatBadge label="기준 패치" value="17.0" />
          </div>
        </header>

        <section className={styles.controlPanel}>
          <div className={styles.tabBar}>
            {GUIDE_TABS.map(({ Icon, key, label, meta }) => (
              <button
                className={activeTab === key ? styles.activeTab : ''}
                key={key}
                onClick={() => {
                  setActiveTab(key)
                  setSearch('')
                }}
                type="button"
              >
                <Icon size={18} />
                <span>{label}</span>
                <small>{meta}</small>
              </button>
            ))}
          </div>
          <label className={styles.searchBox}>
            <Search size={15} />
            <input
              onChange={(event) => setSearch(event.target.value)}
              placeholder={`${activeTabInfo.label} 검색`}
              value={search}
            />
          </label>
        </section>

        <GuideQuickAccess
          favoriteChampions={favoriteChampions}
          onJump={jumpToGuide}
          recentGuides={recentGuides}
        />

        {activeTab === 'traits' && (
          <TraitGuideView
            onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
            query={search}
          />
        )}
        {activeTab === 'items' && (
          <ItemStatsView
            onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
            query={search}
          />
        )}
        {activeTab === 'augments' && <AugmentGuideView query={search} />}
        {activeTab === 'champions' && (
          <ChampionGuideView
            favoriteChampions={favoriteChampions}
            onChampionOpen={(championName) => addRecentGuide({ label: championName, query: championName, tab: 'champions' })}
            onFavoriteToggle={handleFavoriteToggle}
            onItemSelect={(itemName) => jumpToGuide('items', itemName, itemName)}
            query={search}
          />
        )}
      </div>
    </AppLayout>
  )
}

export default Guide
