import {
  SearchRateLimitError,
  type SummonerProfileResponse,
} from '../../../api/searchApi'

export const RECENT_SUMMONER_SEARCHES_KEY = 'tft_recent_searches'
const DEFAULT_TAG_LINE = 'KR1'
const MAX_RECENT_SEARCHES = 5

const TIER_KO: Record<string, string> = {
  BRONZE: '브론즈',
  CHALLENGER: '챌린저',
  DIAMOND: '다이아몬드',
  EMERALD: '에메랄드',
  GOLD: '골드',
  GRANDMASTER: '그랜드마스터',
  IRON: '아이언',
  MASTER: '마스터',
  PLATINUM: '플래티넘',
  SILVER: '실버',
}

export type SummonerSearchErrorState = 'error' | 'notFound' | 'rateLimited'

export interface SummonerSearchTarget {
  gameName: string
  normalized: string
  tagLine: string
}

export interface SummonerSearchStorage {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
}

export type SummonerSearchParseResult =
  | { message: string; ok: false }
  | { ok: true; value: SummonerSearchTarget }

function getBrowserStorage(): SummonerSearchStorage | undefined {
  if (typeof localStorage === 'undefined') {
    return undefined
  }

  return localStorage
}

export function toNormalizedSummonerSearch(gameName: string, tagLine: string): SummonerSearchTarget {
  const normalizedGameName = gameName.trim()
  const normalizedTagLine = tagLine.trim()

  return {
    gameName: normalizedGameName,
    normalized: `${normalizedGameName}#${normalizedTagLine}`,
    tagLine: normalizedTagLine,
  }
}

export function parseSummonerSearchInput(input: string): SummonerSearchParseResult {
  const trimmedInput = input.trim()

  if (!trimmedInput) {
    return { ok: false, message: '소환사명과 태그를 입력해주세요.' }
  }

  const parts = trimmedInput.split('#').map((part) => part.trim())

  if (parts.length > 2) {
    return { ok: false, message: '소환사명#태그 형식으로 입력해주세요.' }
  }

  const gameName = parts[0] ?? ''
  const hasExplicitTag = parts.length === 2
  const tagLine = hasExplicitTag ? parts[1] ?? '' : DEFAULT_TAG_LINE

  if (!gameName || !tagLine) {
    return { ok: false, message: '소환사명과 태그를 모두 입력해주세요.' }
  }

  return {
    ok: true,
    value: toNormalizedSummonerSearch(gameName, tagLine),
  }
}

export function formatSummonerDetailPath(target: Pick<SummonerSearchTarget, 'gameName' | 'tagLine'>): string {
  return `/summoner/${encodeURIComponent(target.gameName)}/${encodeURIComponent(target.tagLine)}`
}

export function formatSummonerTier(tier: string | null | undefined, rank: string | null | undefined): string {
  if (!tier) {
    return '언랭크'
  }

  const tierLabel = TIER_KO[tier] ?? tier

  return rank ? `${tierLabel} ${rank}` : tierLabel
}

export function formatSummonerWinRate(wins: number, losses: number): string {
  const total = wins + losses

  if (total <= 0) {
    return '-'
  }

  return `${Math.round((wins / total) * 100)}%`
}

export function mapSummonerSearchError(error: unknown): SummonerSearchErrorState {
  if (error instanceof SearchRateLimitError) {
    return 'rateLimited'
  }

  if (error instanceof Error) {
    if (error.message === 'NOT_FOUND') return 'notFound'
    if (error.message === 'RATE_LIMITED') return 'rateLimited'
  }

  return 'error'
}

export function getSummonerSearchRetryAfterSeconds(error: unknown): number | null {
  if (error instanceof SearchRateLimitError) {
    return error.retryAfterSeconds
  }

  if (error instanceof Error && error.message === 'RATE_LIMITED') {
    return 120
  }

  return null
}

export function profileToSummonerSearchTarget(profile: SummonerProfileResponse): SummonerSearchTarget {
  return toNormalizedSummonerSearch(profile.gameName, profile.tagLine)
}

export function readRecentSummonerSearches(
  storage: SummonerSearchStorage | undefined = getBrowserStorage(),
): string[] {
  if (!storage) {
    return []
  }

  try {
    const parsed: unknown = JSON.parse(storage.getItem(RECENT_SUMMONER_SEARCHES_KEY) ?? '[]')

    if (!Array.isArray(parsed)) {
      return []
    }

    return parsed
      .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
      .slice(0, MAX_RECENT_SEARCHES)
  } catch {
    return []
  }
}

export function saveRecentSummonerSearch(
  normalizedSearch: string,
  storage: SummonerSearchStorage | undefined = getBrowserStorage(),
): string[] {
  const trimmedSearch = normalizedSearch.trim()
  const currentSearches = readRecentSummonerSearches(storage)

  if (!trimmedSearch) {
    return currentSearches
  }

  const nextSearches = [
    trimmedSearch,
    ...currentSearches.filter((search) => search !== trimmedSearch),
  ].slice(0, MAX_RECENT_SEARCHES)

  if (!storage) {
    return nextSearches
  }

  try {
    storage.setItem(RECENT_SUMMONER_SEARCHES_KEY, JSON.stringify(nextSearches))
  } catch {
    return currentSearches
  }

  return nextSearches
}
