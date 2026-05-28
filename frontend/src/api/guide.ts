import axiosInstance from './axiosInstance'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'
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
  patchVersion: string
  rewards: RewardRow[]
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

function readString(record: Record<string, unknown>, key: string, fallback = '') {
  const value = record[key]
  return typeof value === 'string' ? value : fallback
}

function readNullableString(record: Record<string, unknown>, key: string) {
  const value = record[key]
  return typeof value === 'string' && value.trim() ? value : undefined
}

function readNumber(record: Record<string, unknown>, key: string, fallback = 0) {
  const value = record[key]
  if (typeof value === 'number') return value
  if (typeof value === 'string') return Number(value) || fallback
  return fallback
}

function readStringArray(record: Record<string, unknown>, key: string) {
  const value = record[key]
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
}

function parseJsonRecord(value: unknown) {
  if (isRecord(value)) return value
  if (typeof value !== 'string') return undefined

  try {
    const parsed: unknown = JSON.parse(value)
    return isRecord(parsed) ? parsed : undefined
  } catch {
    return undefined
  }
}

function readDataRecord(entry: GuideEntryResponse) {
  const dataJson = parseJsonRecord(entry.dataJson)
  if (dataJson) return dataJson

  const dataJsonSnake = parseJsonRecord(entry.data_json)
  if (dataJsonSnake) return dataJsonSnake

  return {}
}

function readImageUrl(entry: GuideEntryResponse, data: Record<string, unknown>) {
  return entry.imageUrl || entry.image_url || readString(data, 'imageUrl') || readString(data, 'image_url')
}

function readPatchVersion(entry: GuideEntryResponse) {
  return entry.patchVersion ?? entry.patch_version ?? undefined
}

function readChampionRefs(value: unknown): ChampionRef[] {
  if (!Array.isArray(value)) return []

  return value.filter(isRecord).map((championRef) => ({
    cost: readNumber(championRef, 'cost', 1),
    imageUrl: readString(championRef, 'imageUrl', readString(championRef, 'image_url')),
    name: readString(championRef, 'name'),
  }))
}

function readItemRefs(value: unknown): ItemRef[] {
  if (!Array.isArray(value)) return []

  return value.filter(isRecord).map((itemRef) => ({
    imageUrl: readString(itemRef, 'imageUrl', readString(itemRef, 'image_url')),
    name: readString(itemRef, 'name'),
  }))
}

function readTraitTone(value: unknown): TraitHexBadgeTone | undefined {
  return value === 'gold' || value === 'silver' || value === 'bronze' || value === 'prismatic'
    ? value
    : undefined
}

function readTier(value: unknown): TierBadgeValue {
  return value === 'S' || value === 'A+' || value === 'A' || value === 'B' || value === 'C' || value === 'D'
    ? value
    : 'B'
}

function readChampionCost(value: unknown): ChampionGuide['cost'] {
  return value === 1 || value === 2 || value === 3 || value === 4 || value === 5 ? value : 1
}

function readCombinations(value: unknown): ItemStatGuide['combinations'] {
  if (!Array.isArray(value)) return []

  return value.filter(isRecord).map((combination) => ({
    items: readItemRefs(combination.items),
    label: readString(combination, 'label'),
    note: readString(combination, 'note'),
  }))
}

function readChampionStats(value: unknown): ChampionGuide['stats'] {
  const stats = isRecord(value) ? value : {}

  return {
    ad: readNumber(stats, 'ad'),
    armor: readNumber(stats, 'armor'),
    attackSpeed: readString(stats, 'attackSpeed', readString(stats, 'attack_speed')),
    hp: readNumber(stats, 'hp'),
    mana: readString(stats, 'mana'),
    mr: readNumber(stats, 'mr'),
    range: readNumber(stats, 'range'),
  }
}

function normalizeGuideType(entry: GuideEntryResponse): GuideTab | undefined {
  const guideType = entry.guideType ?? entry.guide_type

  if (guideType === 'TRAIT' || guideType === 'traits') return 'traits'
  if (guideType === 'ITEM' || guideType === 'items') return 'items'
  if (guideType === 'AUGMENT' || guideType === 'augments') return 'augments'
  if (guideType === 'CHAMPION' || guideType === 'champions') return 'champions'
  return undefined
}

function isGuideEntryList(payload: unknown): payload is GuideEntryResponse[] {
  return Array.isArray(payload) && payload.every((entry) => (
    isRecord(entry)
    && typeof entry.id === 'number'
    && typeof entry.name === 'string'
    && ('guideType' in entry || 'guide_type' in entry)
  ))
}

function guideEntriesToCatalog(entries: GuideEntryResponse[], fallbackData: GuideCatalog): GuideCatalog {
  const sortedEntries = [...entries].sort((first, second) => (
    (first.sortOrder ?? first.sort_order ?? 0) - (second.sortOrder ?? second.sort_order ?? 0)
  ))
  const catalog: GuideCatalog = {
    augments: [],
    augmentPlans: fallbackData.augmentPlans,
    champions: [],
    items: [],
    patchVersion: entries.map(readPatchVersion).find(Boolean) ?? fallbackData.patchVersion,
    rewards: fallbackData.rewards,
    traits: [],
  }

  sortedEntries.forEach((entry) => {
    const data = readDataRecord(entry)
    const imageUrl = readImageUrl(entry, data)
    const summary = entry.summary ?? readNullableString(data, 'summary') ?? ''

    if (normalizeGuideType(entry) === 'traits') {
      catalog.traits.push({
        champions: readChampionRefs(data.champions),
        count: readNumber(data, 'count'),
        iconUrl: imageUrl,
        levels: readStringArray(data, 'levels'),
        name: entry.name,
        summary,
        tips: readStringArray(data, 'tips'),
        tone: readTraitTone(data.tone),
        type: readString(data, 'type', '시너지'),
      })
    }

    if (normalizeGuideType(entry) === 'items') {
      catalog.items.push({
        avgPlace: readString(data, 'avgPlace', readString(data, 'avg_place')),
        bestUsers: readChampionRefs(data.bestUsers ?? data.best_users),
        category: readString(data, 'category', '완성 아이템'),
        combinations: readCombinations(data.combinations),
        imageUrl,
        name: entry.name,
        pickRate: readString(data, 'pickRate', readString(data, 'pick_rate')),
        top4: readString(data, 'top4'),
        winRate: readString(data, 'winRate', readString(data, 'win_rate')),
      })
    }

    if (normalizeGuideType(entry) === 'augments') {
      catalog.augments.push({
        avgPlace: readString(data, 'avgPlace', readString(data, 'avg_place')),
        description: readString(data, 'description', summary),
        name: entry.name,
        pickRate: readString(data, 'pickRate', readString(data, 'pick_rate')),
        reward: readString(data, 'reward', '-'),
        tags: readStringArray(data, 'tags'),
        tier: readTier(data.tier),
        type: readString(data, 'type', '공용'),
        winRate: readString(data, 'winRate', readString(data, 'win_rate')),
      })
    }

    if (normalizeGuideType(entry) === 'champions') {
      catalog.champions.push({
        bestItems: readItemRefs(data.bestItems ?? data.best_items),
        cost: readChampionCost(data.cost),
        imageUrl,
        name: entry.name,
        position: readString(data, 'position'),
        role: readString(data, 'role'),
        stats: readChampionStats(data.stats),
        traits: readStringArray(data, 'traits'),
      })
    }
  })

  return catalog
}

function normalizeGuideCatalog(payload: GuideCatalog | GuideEntryResponse[], fallbackData: GuideCatalog): GuideCatalog {
  if (isGuideEntryList(payload)) {
    return guideEntriesToCatalog(payload, fallbackData)
  }

  return {
    augments: Array.isArray(payload.augments) ? payload.augments : fallbackData.augments,
    augmentPlans: Array.isArray(payload.augmentPlans) ? payload.augmentPlans : fallbackData.augmentPlans,
    champions: Array.isArray(payload.champions) ? payload.champions : fallbackData.champions,
    items: Array.isArray(payload.items) ? payload.items : fallbackData.items,
    patchVersion: payload.patchVersion || fallbackData.patchVersion,
    rewards: Array.isArray(payload.rewards) ? payload.rewards : fallbackData.rewards,
    traits: Array.isArray(payload.traits) ? payload.traits : fallbackData.traits,
  }
}

export async function getGuideCatalog(fallbackData: GuideCatalog): Promise<GuideCatalogResult> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<GuideCatalog | GuideEntryResponse[]> | GuideCatalog | GuideEntryResponse[]>('/guide')
    return { data: normalizeGuideCatalog(unwrapApiResponse(data), fallbackData), source: 'api' }
  } catch {
    return { data: fallbackData, source: 'fallback' }
  }
}

export async function getGuideTabItems<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): Promise<GuideTabItems[T]> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<GuideTabItems[T]> | GuideTabItems[T]>(`/guide/${params.tab}`, { params })
    return unwrapApiResponse(data)
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
