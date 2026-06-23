import {
  DEFAULT_GUIDE_PAGE_SIZE,
  GUIDE_SAMPLE_PAGE_COUNT,
  GUIDE_SAMPLE_VARIANTS,
  type ChampionGuide,
  type GuideCatalog,
  type GuideListQuery,
  type GuidePage,
  type GuideTab,
  type GuideTabItems,
  type AugmentGuide,
  type ItemStatGuide,
  type TraitGuide,
} from './guideTypes'

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

function normalizePositiveInteger(value: number | undefined, fallback: number) {
  return Number.isFinite(value) && value !== undefined && value > 0
    ? Math.floor(value)
    : fallback
}

function normalizeNonNegativeInteger(value: number) {
  return Number.isFinite(value) ? Math.max(0, Math.floor(value)) : 0
}

export function getTotalPages(totalItems: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  const safePageSize = normalizePositiveInteger(pageSize, DEFAULT_GUIDE_PAGE_SIZE)
  const safeTotalItems = normalizeNonNegativeInteger(totalItems)

  return Math.max(1, Math.ceil(safeTotalItems / safePageSize))
}

export function getPageItems<T>(items: T[], page: number, pageSize = DEFAULT_GUIDE_PAGE_SIZE) {
  if (items.length === 0) return []

  const safePage = normalizePositiveInteger(page, 1)
  const safePageSize = normalizePositiveInteger(pageSize, DEFAULT_GUIDE_PAGE_SIZE)
  const startIndex = (safePage - 1) * safePageSize

  return items.slice(startIndex, startIndex + safePageSize)
}

function getGuideSearchFields(tab: GuideTab, item: GuideTabItems[GuideTab][number]) {
  if (tab === 'traits') {
    const traitGuide = item as TraitGuide
    return [
      traitGuide.name,
      traitGuide.summary,
      traitGuide.type,
      ...traitGuide.champions.map((championRef) => championRef.name),
    ]
  }

  if (tab === 'items') {
    const itemStat = item as ItemStatGuide
    return [
      itemStat.name,
      itemStat.category,
      ...itemStat.bestUsers.map((championRef) => championRef.name),
      ...itemStat.combinations.map((combination) => combination.label),
    ]
  }

  if (tab === 'augments') {
    const augment = item as AugmentGuide
    return [augment.name, augment.description, ...augment.tags]
  }

  const championGuide = item as ChampionGuide
  return [
    championGuide.name,
    championGuide.role,
    championGuide.position,
    ...championGuide.traits,
    ...championGuide.bestItems.map((itemRef) => itemRef.name),
  ]
}

function getFallbackGuideItems<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
) {
  const fallbackByTab: GuideTabItems = {
    augments: fallbackData.augments,
    champions: fallbackData.champions,
    items: fallbackData.items,
    traits: fallbackData.traits,
  }
  let items = [...fallbackByTab[params.tab]] as GuideTabItems[T]

  if (params.query?.trim()) {
    items = items.filter((item) => matchesSearch(params.query ?? '', getGuideSearchFields(params.tab, item))) as GuideTabItems[T]
  }

  if (params.tab === 'champions' && params.cost && params.cost !== 'all') {
    items = (items as ChampionGuide[]).filter((championGuide) => championGuide.cost === params.cost) as GuideTabItems[T]
  }

  return items
}

export function getFallbackGuideTabPage<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): GuidePage<GuideTabItems[T][number]> {
  const page = normalizePositiveInteger(params.page, 1)
  const pageSize = normalizePositiveInteger(params.pageSize, DEFAULT_GUIDE_PAGE_SIZE)
  const items = getFallbackGuideItems(params, fallbackData)

  return {
    items: getPageItems(items as GuideTabItems[T][number][], page, pageSize),
    page,
    pageSize,
    totalItems: items.length,
    totalPages: getTotalPages(items.length, pageSize),
  }
}
