import { getChampionShortName, getItemName, getTraitName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import { tftItemIconUrl } from '../../../api/communityDragonAssets'
import { costLimitForLevel } from './deckUtils'
import type { ChampionItemSummary, ChampionSummary, MetaDeck } from '../../Dashboard/dashboardData'

export type SortKey = 'rank' | 'top4' | 'avgPlace' | 'pickRate'
export type SortDir = 'asc' | 'desc'

export interface HeroAugmentInfo {
  championId: string
  championName: string
  augmentName: string
}

export interface HeroChampInfo {
  imageUrl: string
  name: string
}

const NON_SHOP_CHAMPION_NAMES = new Set(['ElderDragon', 'IvernMinion', 'Summon'])

export function numVal(s: string): number {
  return parseFloat(s.replace('%', ''))
}

export function deckDisplayName(deck: MetaDeck, locale: TFTLocale | undefined): string {
  const traitName = deck.traits.length > 0 ? getTraitName(deck.traits[0].name, locale) : ''
  const carries = deck.champions
    .filter((c) => (c.recommendedItems?.length ?? 0) > 0)
    .slice(0, 2)
    .map((c) => getChampionShortName(c.imageUrl, locale, c.name))
  const parts = [traitName, ...carries].filter(Boolean)
  return parts.length > 0 ? parts.join(' ') : deck.name
}

export function inferDeckLevel(deck: MetaDeck): number {
  const carries = deck.champions.filter((c) => (c.recommendedItems?.length ?? 0) > 0)
  const maxCarryCost = carries.reduce((max, c) => Math.max(max, c.cost ?? 0), 0)
  if (maxCarryCost >= 5) return 9
  if (maxCarryCost >= 4) return 8
  if (maxCarryCost === 3) return 7
  return 8
}

export function shopChampions(deck: MetaDeck): ChampionSummary[] {
  const costLimit = costLimitForLevel(inferDeckLevel(deck))
  return deck.champions.filter(
    (c) => !NON_SHOP_CHAMPION_NAMES.has(c.name) && (c.cost ?? 0) <= costLimit,
  )
}

export function sortDecks(decks: MetaDeck[], key: SortKey, dir: SortDir): MetaDeck[] {
  return [...decks].sort((a, b) => {
    const av = key === 'rank' ? a.rank : numVal(a[key])
    const bv = key === 'rank' ? b.rank : numVal(b[key])
    const naturalAsc = key === 'avgPlace' || key === 'rank'
    const base = av < bv ? -1 : av > bv ? 1 : 0
    return (naturalAsc ? base : -base) * (dir === 'asc' ? 1 : -1)
  })
}

export function resolveItems(c: ChampionSummary, locale: TFTLocale | undefined): ChampionItemSummary[] {
  if (c.items && c.items.length > 0) return c.items
  return (c.recommendedItems ?? []).map((id) => ({
    imageUrl: tftItemIconUrl(id),
    name: getItemName(id, locale),
  }))
}

export function parseHeroAugments(json: string | null): HeroAugmentInfo[] {
  if (!json) return []
  try { return JSON.parse(json) as HeroAugmentInfo[] } catch { return [] }
}

export function parseChampions(json: string | null): HeroChampInfo[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json) as { characterId?: string; imageUrl?: string; name?: string }[]
    return arr.filter((c) => c.imageUrl).map((c) => ({ imageUrl: c.imageUrl!, name: c.name ?? '' }))
  } catch { return [] }
}
