import { isRecord } from './apiResponse'
import { getChampionApiName } from './cdragonLocale'
import { tftChampSquareUrl } from './communityDragonAssets'
import { isDisplayableGuideTag, sanitizeGuideText } from './guideText'
import type { TraitHexBadgeTone } from '../types/badges'
import { getTotalPages } from './guideFallback'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  type AugmentGuide,
  type ChampionGuide,
  type ChampionRef,
  type GuideCatalog,
  type GuideEntryResponse,
  type GuideListQuery,
  type GuidePage,
  type GuideTab,
  type GuideTabItems,
  type ItemRef,
  type ItemStatGuide,
  type SpecialUnitRef,
  type TraitGuide,
  type TraitTierEffect,
} from './guideTypes'

const GUIDE_ENTRY_NAME_COLLATOR = new Intl.Collator('ko-KR', {
  numeric: true,
  sensitivity: 'base',
})

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

function readDisplayTags(record: Record<string, unknown>, key: string) {
  return readStringArray(record, key)
    .map(sanitizeGuideText)
    .filter(isDisplayableGuideTag)
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

function normalizeChampionImageUrl(imageUrl: string) {
  const apiName = getChampionApiName(imageUrl)
  return apiName ? tftChampSquareUrl(apiName) : imageUrl
}

function readPatchVersion(entry: GuideEntryResponse) {
  return entry.patchVersion ?? entry.patch_version ?? undefined
}

function readTargetKey(entry: GuideEntryResponse) {
  if (typeof entry.targetKey === 'string') return entry.targetKey
  if (typeof entry.target_key === 'string') return entry.target_key
  return undefined
}

function readChampionRefs(value: unknown): ChampionRef[] {
  if (!Array.isArray(value)) return []

  return value.filter(isRecord).map((championRef) => ({
    cost: readNumber(championRef, 'cost', 1),
    imageUrl: normalizeChampionImageUrl(readString(championRef, 'imageUrl', readString(championRef, 'image_url'))),
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

function readSpecialUnitRefs(value: unknown): SpecialUnitRef[] {
  if (!Array.isArray(value)) return []

  return value.filter(isRecord).map((specialUnitRef) => ({
    imageUrl: readString(specialUnitRef, 'imageUrl', readString(specialUnitRef, 'image_url')),
    name: readString(specialUnitRef, 'name'),
    note: readNullableString(specialUnitRef, 'note'),
  }))
}

function readTraitTone(value: unknown): TraitHexBadgeTone | undefined {
  return value === 'gold' || value === 'silver' || value === 'bronze' || value === 'prismatic'
    ? value
    : undefined
}

function isTraitTierEffect(payload: unknown): payload is TraitTierEffect {
  return isRecord(payload)
    && typeof payload.description === 'string'
    && typeof payload.level === 'string'
}

function readTraitTierEffects(value: unknown): TraitTierEffect[] {
  return Array.isArray(value) ? value.filter(isTraitTierEffect) : []
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

function readOptionalNumber(record: Record<string, unknown>, key: string) {
  const value = record[key]
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}

function normalizePositiveInteger(value: number | undefined, fallback: number) {
  return Number.isFinite(value) && value !== undefined && value > 0
    ? Math.floor(value)
    : fallback
}

function normalizeNonNegativeInteger(value: number | undefined) {
  return Number.isFinite(value) && value !== undefined
    ? Math.max(0, Math.floor(value))
    : 0
}

function readPageItems(payload: unknown): unknown[] | undefined {
  if (Array.isArray(payload)) return payload
  if (!isRecord(payload)) return undefined

  const itemKeys = ['items', 'content', 'data', 'results']
  const itemValue = itemKeys.map((key) => payload[key]).find(Array.isArray)

  return itemValue
}

function readPageNumber(payload: unknown, fallbackPage: number) {
  if (!isRecord(payload)) return fallbackPage

  const page = readOptionalNumber(payload, 'page') ?? readOptionalNumber(payload, 'currentPage')
  if (page !== undefined) return Math.max(1, page)

  const zeroBasedPage = readOptionalNumber(payload, 'number')
  return zeroBasedPage !== undefined ? Math.max(1, zeroBasedPage + 1) : fallbackPage
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

function readCatalogEntries(payload: unknown): GuideEntryResponse[] | undefined {
  if (!isRecord(payload)) return undefined
  return isGuideEntryList(payload.entries) ? payload.entries : undefined
}

function isStringList(payload: unknown): payload is string[] {
  return Array.isArray(payload) && payload.every((item) => typeof item === 'string')
}

function isItemRef(payload: unknown): payload is ItemRef {
  return isRecord(payload)
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
}

function isChampionRef(payload: unknown): payload is ChampionRef {
  return isRecord(payload)
    && typeof payload.cost === 'number'
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
}

function isSpecialUnitRef(payload: unknown): payload is SpecialUnitRef {
  return isRecord(payload)
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
    && (!('note' in payload) || typeof payload.note === 'string')
}

function isTraitGuide(payload: unknown): payload is TraitGuide {
  return isRecord(payload)
    && Array.isArray(payload.champions)
    && payload.champions.every(isChampionRef)
    && typeof payload.count === 'number'
    && typeof payload.iconUrl === 'string'
    && isStringList(payload.levels)
    && typeof payload.name === 'string'
    && (!('specialUnits' in payload) || (
      Array.isArray(payload.specialUnits) && payload.specialUnits.every(isSpecialUnitRef)
    ))
    && typeof payload.summary === 'string'
    && (!('tierEffects' in payload) || (
      Array.isArray(payload.tierEffects) && payload.tierEffects.every(isTraitTierEffect)
    ))
    && isStringList(payload.tips)
    && (!('targetKey' in payload) || typeof payload.targetKey === 'string')
    && typeof payload.type === 'string'
    && (!('variant' in payload) || typeof payload.variant === 'string')
}

function isItemCombination(payload: unknown): payload is ItemStatGuide['combinations'][number] {
  return isRecord(payload)
    && Array.isArray(payload.items)
    && payload.items.every(isItemRef)
    && typeof payload.label === 'string'
    && typeof payload.note === 'string'
}

function isItemStatGuide(payload: unknown): payload is ItemStatGuide {
  return isRecord(payload)
    && Array.isArray(payload.bestUsers)
    && payload.bestUsers.every(isChampionRef)
    && typeof payload.category === 'string'
    && Array.isArray(payload.combinations)
    && payload.combinations.every(isItemCombination)
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
    && (!('targetKey' in payload) || typeof payload.targetKey === 'string')
}

function isAugmentGuide(payload: unknown): payload is AugmentGuide {
  return isRecord(payload)
    && typeof payload.description === 'string'
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
    && (!('targetKey' in payload) || typeof payload.targetKey === 'string')
    && isStringList(payload.tags)
}

function isChampionStats(payload: unknown): payload is ChampionGuide['stats'] {
  return isRecord(payload)
    && typeof payload.ad === 'number'
    && typeof payload.armor === 'number'
    && typeof payload.attackSpeed === 'string'
    && typeof payload.hp === 'number'
    && typeof payload.mana === 'string'
    && typeof payload.mr === 'number'
    && typeof payload.range === 'number'
}

function isChampionGuide(payload: unknown): payload is ChampionGuide {
  return isRecord(payload)
    && Array.isArray(payload.bestItems)
    && payload.bestItems.every(isItemRef)
    && readChampionCost(payload.cost) === payload.cost
    && typeof payload.imageUrl === 'string'
    && typeof payload.name === 'string'
    && typeof payload.position === 'string'
    && typeof payload.role === 'string'
    && isChampionStats(payload.stats)
    && (!('targetKey' in payload) || typeof payload.targetKey === 'string')
    && isStringList(payload.traits)
}

function isGuideTabItems<T extends GuideTab>(tab: T, rawItems: unknown[]): rawItems is GuideTabItems[T] {
  if (tab === 'traits') return rawItems.every(isTraitGuide)
  if (tab === 'items') return rawItems.every(isItemStatGuide)
  if (tab === 'augments') return rawItems.every(isAugmentGuide)
  return rawItems.every(isChampionGuide)
}

function guideEntriesToCatalog(entries: GuideEntryResponse[], fallbackData: GuideCatalog): GuideCatalog {
  const sortedEntries = [...entries].sort((first, second) => (
    GUIDE_ENTRY_NAME_COLLATOR.compare(first.name, second.name)
      || (first.sortOrder ?? first.sort_order ?? 0) - (second.sortOrder ?? second.sort_order ?? 0)
      || first.id - second.id
  ))
  const catalog: GuideCatalog = {
    augments: [],
    champions: [],
    items: [],
    patchVersion: entries.map(readPatchVersion).find(Boolean) ?? fallbackData.patchVersion,
    traits: [],
  }

  sortedEntries.forEach((entry) => {
    const data = readDataRecord(entry)
    const imageUrl = readImageUrl(entry, data)
    const summary = sanitizeGuideText(entry.summary ?? readNullableString(data, 'summary') ?? '')

    if (normalizeGuideType(entry) === 'traits') {
      catalog.traits.push({
        champions: readChampionRefs(data.champions),
        count: readNumber(data, 'count'),
        iconUrl: imageUrl,
        levels: readStringArray(data, 'levels'),
        name: entry.name,
        specialUnits: readSpecialUnitRefs(data.specialUnits ?? data.special_units),
        summary,
        targetKey: readTargetKey(entry),
        tierEffects: readTraitTierEffects(data.tierEffects),
        tips: readStringArray(data, 'tips'),
        tone: readTraitTone(data.tone),
        type: readString(data, 'type', '시너지'),
        variant: readNullableString(data, 'variant'),
      })
    }

    if (normalizeGuideType(entry) === 'items') {
      catalog.items.push({
        bestUsers: readChampionRefs(data.bestUsers ?? data.best_users),
        category: readString(data, 'category', '완성 아이템'),
        combinations: readCombinations(data.combinations),
        description: sanitizeGuideText(readString(data, 'description', summary)),
        imageUrl,
        name: entry.name,
        targetKey: readTargetKey(entry),
      })
    }

    if (normalizeGuideType(entry) === 'augments') {
      catalog.augments.push({
        description: sanitizeGuideText(readString(data, 'description', summary)),
        imageUrl,
        name: entry.name,
        tags: readDisplayTags(data, 'tags'),
        targetKey: readTargetKey(entry),
      })
    }

    if (normalizeGuideType(entry) === 'champions') {
      catalog.champions.push({
        bestItems: readItemRefs(data.bestItems ?? data.best_items),
        cost: readChampionCost(data.cost),
        imageUrl: normalizeChampionImageUrl(imageUrl),
        name: entry.name,
        position: readString(data, 'position'),
        role: readString(data, 'role'),
        stats: readChampionStats(data.stats),
        targetKey: readTargetKey(entry),
        traits: readStringArray(data, 'traits'),
      })
    }
  })

  return catalog
}

export function hasGuidePayloadData(payload: unknown): boolean {
  if (Array.isArray(payload)) {
    if (!isGuideEntryList(payload)) return false
    return payload.some((entry) => normalizeGuideType(entry) !== undefined)
  }

  const entries = readCatalogEntries(payload)
  if (entries) {
    return entries.some((entry) => normalizeGuideType(entry) !== undefined)
  }

  if (!isRecord(payload)) return false

  return ['traits', 'items', 'augments', 'champions'].some((key) => {
    const value = payload[key]
    return Array.isArray(value) && value.length > 0
  })
}

export function normalizeGuideCatalog(payload: unknown, fallbackData: GuideCatalog): GuideCatalog {
  if (isGuideEntryList(payload)) {
    return guideEntriesToCatalog(payload, fallbackData)
  }

  const entries = readCatalogEntries(payload)
  if (entries && isRecord(payload)) {
    const catalog = guideEntriesToCatalog(entries, fallbackData)
    return {
      ...catalog,
      patchVersion: typeof payload.patchVersion === 'string' && payload.patchVersion
        ? payload.patchVersion
        : catalog.patchVersion,
    }
  }

  if (!isRecord(payload)) return fallbackData

  return {
    augments: Array.isArray(payload.augments) && payload.augments.every(isAugmentGuide)
      ? payload.augments
      : fallbackData.augments,
    champions: Array.isArray(payload.champions) && payload.champions.every(isChampionGuide)
      ? payload.champions
      : fallbackData.champions,
    items: Array.isArray(payload.items) && payload.items.every(isItemStatGuide)
      ? payload.items
      : fallbackData.items,
    patchVersion: typeof payload.patchVersion === 'string' && payload.patchVersion
      ? payload.patchVersion
      : fallbackData.patchVersion,
    traits: Array.isArray(payload.traits) && payload.traits.every(isTraitGuide)
      ? payload.traits
      : fallbackData.traits,
  }
}

function normalizeGuideTabItems<T extends GuideTab>(
  tab: T,
  rawItems: unknown[],
  fallbackData: GuideCatalog,
): GuideTabItems[T] | undefined {
  if (isGuideEntryList(rawItems)) {
    return guideEntriesToCatalog(rawItems, fallbackData)[tab] as GuideTabItems[T]
  }

  if (!isGuideTabItems(tab, rawItems)) return undefined

  return rawItems
}

export function normalizeGuideTabPage<T extends GuideTab>(
  payload: unknown,
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): GuidePage<GuideTabItems[T][number]> | undefined {
  const rawItems = readPageItems(payload)
  if (!rawItems) return undefined

  const items = normalizeGuideTabItems(params.tab, rawItems, fallbackData)
  if (!items) return undefined

  const page = readPageNumber(payload, params.page ?? 1)
  const rawPageSize = isRecord(payload)
    ? readOptionalNumber(payload, 'pageSize') ?? readOptionalNumber(payload, 'size') ?? params.pageSize ?? DEFAULT_GUIDE_PAGE_SIZE
    : params.pageSize ?? DEFAULT_GUIDE_PAGE_SIZE
  const pageSize = normalizePositiveInteger(rawPageSize, DEFAULT_GUIDE_PAGE_SIZE)

  const rawTotalItems = isRecord(payload)
    ? readOptionalNumber(payload, 'totalItems') ?? readOptionalNumber(payload, 'totalElements') ?? readOptionalNumber(payload, 'total') ?? items.length
    : items.length
  const totalItems = normalizeNonNegativeInteger(rawTotalItems)

  const rawTotalPages = isRecord(payload)
    ? readOptionalNumber(payload, 'totalPages')
    : undefined
  const totalPages = isRecord(payload)
    ? normalizePositiveInteger(rawTotalPages, getTotalPages(totalItems, pageSize))
    : getTotalPages(totalItems, pageSize)

  return {
    items: items as GuideTabItems[T][number][],
    page,
    pageSize,
    totalItems,
    totalPages,
  }
}
