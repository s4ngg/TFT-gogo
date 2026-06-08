import { getChampionDetail } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { ChampionSummary } from '../../Dashboard/dashboardData'

export function isCarry(champ: ChampionSummary): boolean {
  return (champ.recommendedItems?.length ?? 0) > 0
}

export function getCost(champ: ChampionSummary, locale: TFTLocale | undefined): number {
  return getChampionDetail(champ.imageUrl, locale)?.cost ?? champ.cost ?? 0
}

export function getRange(champ: ChampionSummary, locale: TFTLocale | undefined): number {
  return getChampionDetail(champ.imageUrl, locale)?.range ?? 1
}

export function getRole(champ: ChampionSummary, locale: TFTLocale | undefined): string {
  return getChampionDetail(champ.imageUrl, locale)?.role?.toLowerCase() ?? ''
}

export function isFrontline(champ: ChampionSummary, locale: TFTLocale | undefined): boolean {
  const role = getRole(champ, locale)
  return getRange(champ, locale) <= 2 || role.includes('tank') || role.includes('fighter')
}

export function isBackline(champ: ChampionSummary, locale: TFTLocale | undefined): boolean {
  const role = getRole(champ, locale)
  return getRange(champ, locale) >= 4 || role.includes('caster') || role.includes('marksman')
}

export function computeTraitCounts(
  champions: ChampionSummary[],
  locale: TFTLocale | undefined,
): Map<string, number> {
  const counts = new Map<string, number>()
  if (!locale) return counts
  champions.forEach((champ) => {
    const detail = getChampionDetail(champ.imageUrl, locale)
    detail?.traits.forEach((trait) => {
      counts.set(trait, (counts.get(trait) ?? 0) + 1)
    })
  })
  return counts
}
