import type { TraitHexBadgeTone } from '../types/badges'

export type GuideTab = 'traits' | 'items' | 'augments' | 'champions'
export type ChampionCostFilter = 'all' | 1 | 2 | 3 | 4 | 5
export type AugmentPlanKey = 'fast8' | 'reroll' | 'flex'
export type SortDir = 'asc' | 'desc'

export const DEFAULT_GUIDE_PAGE_SIZE = 5
export const TRAIT_PAGE_SIZE = 6
export const CHAMPION_PAGE_SIZE = 10
export const GUIDE_SAMPLE_PAGE_COUNT = 7
export const PAGE_NUMBER_WINDOW = 5
export const GUIDE_SAMPLE_VARIANTS = ['운영', '고점', '안정', '전환', '리롤', '연승', '후반']

export interface GuideTabMeta {
  key: GuideTab
  label: string
  meta: string
}

export const GUIDE_TABS: GuideTabMeta[] = [
  { key: 'traits', label: '시너지', meta: '설명 + 필요 챔피언' },
  { key: 'items', label: '아이템', meta: '효과 + 조합식' },
  { key: 'augments', label: '증강체', meta: '효과 + 보상' },
  { key: 'champions', label: '챔피언', meta: '스탯 + 특성' },
]

export interface ChampionRef {
  cost: number
  imageUrl: string
  name: string
}

export interface ItemRef {
  imageUrl: string
  name: string
}

export interface TraitTierEffect {
  description: string
  level: string
}

export interface TraitGuide {
  champions: ChampionRef[]
  count: number
  iconUrl: string
  levels: string[]
  name: string
  summary: string
  tierEffects?: TraitTierEffect[]
  tips: string[]
  tone?: TraitHexBadgeTone
  type: string
  variant?: string
}

export interface ItemStatGuide {
  avgPlace?: string
  bestUsers: ChampionRef[]
  category: string
  combinations: {
    items: ItemRef[]
    label: string
    note: string
  }[]
  description?: string
  imageUrl: string
  name: string
  pickRate?: string
  top4?: string
  winRate?: string
}

export interface AugmentGuide {
  avgPlace?: string
  description: string
  imageUrl: string
  name: string
  pickRate?: string
  tags: string[]
  winRate?: string
}

export interface AugmentPlan {
  key: AugmentPlanKey
  label: string
  stages: {
    choice: string
    focus: string
    stage: string
  }[]
}

export interface ChampionGuide {
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

export interface RecentGuide {
  label: string
  query: string
  tab: GuideTab
}

export interface GuideCatalog {
  augments: AugmentGuide[]
  augmentPlans: AugmentPlan[]
  champions: ChampionGuide[]
  items: ItemStatGuide[]
  patchVersion: string
  traits: TraitGuide[]
}

export type GuideEntryType = 'TRAIT' | 'ITEM' | 'AUGMENT' | 'CHAMPION'

export interface GuideEntryResponse {
  dataJson?: Record<string, unknown> | string | null
  data_json?: Record<string, unknown> | string | null
  guideType?: GuideEntryType | GuideTab
  guide_type?: GuideEntryType | GuideTab
  id: number
  imageUrl?: string | null
  image_url?: string | null
  name: string
  patchVersion?: string | null
  patch_version?: string | null
  sortOrder?: number | null
  sort_order?: number | null
  summary?: string | null
  targetKey?: string | null
  target_key?: string | null
}

export type GuideDataSource = 'api' | 'fallback' | 'placeholder'

export interface GuideCatalogResult {
  data: GuideCatalog
  source: GuideDataSource
}

export interface GuidePage<T> {
  items: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface GuideTabPageResult<T extends GuideTab> {
  data: GuidePage<GuideTabItems[T][number]>
  source: GuideDataSource
}

export interface GuideListQuery {
  cost?: ChampionCostFilter
  page?: number
  pageSize?: number
  query?: string
  tab: GuideTab
}

export interface GuideTabItems {
  augments: AugmentGuide[]
  champions: ChampionGuide[]
  items: ItemStatGuide[]
  traits: TraitGuide[]
}
