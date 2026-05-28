import axiosInstance from './axiosInstance'
import type { TierBadgeValue } from '../components/common/TierBadge'
import type { TraitHexBadgeTone } from '../components/common/TraitHexBadge'

export type GuideTab = 'traits' | 'items' | 'augments' | 'champions'
export type ChampionCostFilter = 'all' | 1 | 2 | 3 | 4 | 5
export type AugmentPlanKey = 'fast8' | 'reroll' | 'flex'
export type MetricSortKey = 'avgPlace' | 'pickRate' | 'top4' | 'winRate'
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
  { key: 'items', label: '아이템', meta: '승률 + 조합 추천' },
  { key: 'augments', label: '증강체', meta: '티어표 + 보상표' },
  { key: 'champions', label: '챔피언', meta: '스탯 + 3신기' },
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

export interface TraitGuide {
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

export interface ItemStatGuide {
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

export interface AugmentGuide {
  avgPlace: string
  description: string
  name: string
  pickRate: string
  reward: string
  tags: string[]
  tier: TierBadgeValue
  type: string
  winRate: string
}

export interface RewardRow {
  condition: string
  reward: string
  stage: string
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

export interface SortableMetricItem {
  avgPlace: string
  pickRate: string
  top4?: string
  winRate: string
}

export interface GuideCatalog {
  augments: AugmentGuide[]
  augmentPlans: AugmentPlan[]
  champions: ChampionGuide[]
  items: ItemStatGuide[]
  rewards: RewardRow[]
  traits: TraitGuide[]
}

export type GuideDataSource = 'api' | 'fallback'

export interface GuideCatalogResult {
  data: GuideCatalog
  source: GuideDataSource
}

export interface GuideListQuery {
  cost?: ChampionCostFilter
  page?: number
  pageSize?: number
  query?: string
  sortDir?: SortDir
  sortKey?: MetricSortKey
  tab: GuideTab
}

export interface GuideTabItems {
  augments: AugmentGuide[]
  champions: ChampionGuide[]
  items: ItemStatGuide[]
  traits: TraitGuide[]
}

export function expandGuideSamples<T extends { name: string }>(
  samples: T[],
  pageSize: number,
  formatName: (name: string, variant: string, copyIndex: number) => string,
) {
  if (samples.length === 0) return []

  const targetCount = pageSize * GUIDE_SAMPLE_PAGE_COUNT
  if (samples.length >= targetCount) return samples.slice(0, targetCount)

  return Array.from({ length: targetCount }, (_, index) => {
    if (index < samples.length) return samples[index]

    const copyIndex = index - samples.length
    const source = samples[copyIndex % samples.length]
    const variant = GUIDE_SAMPLE_VARIANTS[Math.floor(copyIndex / samples.length) % GUIDE_SAMPLE_VARIANTS.length]

    return {
      ...source,
      name: formatName(source.name, variant, copyIndex + 1),
    }
  })
}

export function normalizeText(value: string) {
  return value.toLowerCase().replace(/\s/g, '')
}

export function matchesSearch(query: string, fields: string[]) {
  const normalizedQuery = normalizeText(query.trim())
  if (!normalizedQuery) return true

  return fields.some((field) => normalizeText(field).includes(normalizedQuery))
}

export function parseMetric(value: string) {
  return Number(value.replace(/[^\d.-]/g, '')) || 0
}

export function sortByMetric<T extends SortableMetricItem>(items: T[], sortKey: MetricSortKey, sortDir: SortDir) {
  return [...items].sort((a, b) => {
    const first = parseMetric(sortKey === 'top4' ? a.top4 ?? '0' : a[sortKey])
    const second = parseMetric(sortKey === 'top4' ? b.top4 ?? '0' : b[sortKey])
    const base = first < second ? -1 : first > second ? 1 : 0

    return sortDir === 'asc' ? base : -base
  })
}

export function getTotalPages(totalItems: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

export function getPageItems<T>(items: T[], page: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  const startIndex = (page - 1) * pageSize
  return items.slice(startIndex, startIndex + pageSize)
}

export async function getGuideCatalog(fallbackData: GuideCatalog): Promise<GuideCatalogResult> {
  try {
    const { data } = await axiosInstance.get<GuideCatalog>('/guide')
    return { data, source: 'api' }
  } catch {
    return { data: fallbackData, source: 'fallback' }
  }
}

export async function getGuideTabItems<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): Promise<GuideTabItems[T]> {
  try {
    const { data } = await axiosInstance.get<GuideTabItems[T]>(`/guide/${params.tab}`, { params })
    return data
  } catch {
    const fallbackByTab: GuideTabItems = {
      augments: fallbackData.augments,
      champions: fallbackData.champions,
      items: fallbackData.items,
      traits: fallbackData.traits,
    }

    return fallbackByTab[params.tab]
  }
}
