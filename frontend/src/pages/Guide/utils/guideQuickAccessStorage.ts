import {
  GUIDE_TABS,
  type GuideTab,
  type RecentGuide,
} from '../../../api/guide'

const FAVORITE_CHAMPIONS_STORAGE_KEY = 'tftgogo-guide-favorite-champions'
const RECENT_GUIDES_STORAGE_KEY = 'tftgogo-guide-recent-guides'
const MAX_FAVORITE_CHAMPIONS = 12
const MAX_RECENT_GUIDES = 6
const GUIDE_TAB_KEYS = new Set<GuideTab>(GUIDE_TABS.map((tab) => tab.key))

interface GuideQuickAccessStorage {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
}

function getStorage(): GuideQuickAccessStorage | undefined {
  if (typeof window === 'undefined') return undefined

  try {
    return window.localStorage
  } catch {
    return undefined
  }
}

function parseStorageValue(key: string, storage = getStorage()): unknown {
  if (!storage) return undefined

  try {
    const rawValue = storage.getItem(key)
    if (!rawValue) return undefined
    return JSON.parse(rawValue) as unknown
  } catch {
    return undefined
  }
}

function writeStorageValue(key: string, value: unknown, storage = getStorage()) {
  if (!storage) return

  try {
    storage.setItem(key, JSON.stringify(value))
  } catch {
    // localStorage quota or browser privacy settings should not break the guide page.
  }
}

function getNormalizedText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

export function normalizeFavoriteChampions(value: unknown) {
  if (!Array.isArray(value)) return []

  const favoriteChampions: string[] = []
  value.forEach((item) => {
    const championName = getNormalizedText(item)
    if (!championName || favoriteChampions.includes(championName)) return
    favoriteChampions.push(championName)
  })

  return favoriteChampions.slice(0, MAX_FAVORITE_CHAMPIONS)
}

function isRecentGuideCandidate(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object'
}

export function normalizeRecentGuides(value: unknown) {
  if (!Array.isArray(value)) return []

  const recentGuides: RecentGuide[] = []
  value.forEach((item) => {
    if (!isRecentGuideCandidate(item)) return

    const query = getNormalizedText(item.query)
    const tab = item.tab
    if (!query || typeof tab !== 'string' || !GUIDE_TAB_KEYS.has(tab as GuideTab)) return

    const guide: RecentGuide = {
      label: getNormalizedText(item.label) || query,
      query,
      tab: tab as GuideTab,
    }
    const hasSameGuide = recentGuides.some((current) => (
      current.query === guide.query && current.tab === guide.tab
    ))
    if (!hasSameGuide) recentGuides.push(guide)
  })

  return recentGuides.slice(0, MAX_RECENT_GUIDES)
}

export function readFavoriteChampions(storage?: GuideQuickAccessStorage) {
  return normalizeFavoriteChampions(parseStorageValue(FAVORITE_CHAMPIONS_STORAGE_KEY, storage))
}

export function readRecentGuides(storage?: GuideQuickAccessStorage) {
  return normalizeRecentGuides(parseStorageValue(RECENT_GUIDES_STORAGE_KEY, storage))
}

export function writeFavoriteChampions(favoriteChampions: string[], storage?: GuideQuickAccessStorage) {
  writeStorageValue(
    FAVORITE_CHAMPIONS_STORAGE_KEY,
    normalizeFavoriteChampions(favoriteChampions),
    storage,
  )
}

export function writeRecentGuides(recentGuides: RecentGuide[], storage?: GuideQuickAccessStorage) {
  writeStorageValue(
    RECENT_GUIDES_STORAGE_KEY,
    normalizeRecentGuides(recentGuides),
    storage,
  )
}
