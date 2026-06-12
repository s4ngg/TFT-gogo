import { getChampionName, getTraitName, type TFTLocale } from '../../../api/cdragonLocale'
import type { AdminDeck } from '../../../api/adminApi'

export const BOARD_ROWS = 4
export const BOARD_COLS = 7
export const BOARD_LEVELS = [5, 6, 7, 8, 9, 10]
export const COST_COLORS: Record<number, string> = { 1: '#8e9497', 2: '#4ade80', 3: '#60a5fa', 4: '#c084fc', 5: '#f9c860' }

export interface CellPos { row: number; col: number; items?: string[] }
export interface ChampInfo { apiName: string; name: string; imageUrl: string; cost: number }

export function isCompleteItem(key: string): boolean {
  const k = key.toLowerCase()
  if (!k.startsWith('tft_item_')) return false
  return !k.includes('emptybag') && !k.includes('radiant') && !k.includes('artifact')
    && !k.includes('support') && !k.includes('emblem') && !k.includes('trait')
    && !k.includes('consumable') && !k.includes('temporary') && !k.includes('ornn')
    && !k.endsWith('bfsword') && !k.endsWith('recurvebow') && !k.endsWith('needlesslylargerod')
    && !k.endsWith('tearofthegoddess') && !k.endsWith('chainvest') && !k.endsWith('negatroncloak')
    && !k.endsWith('giantsbelt') && !k.endsWith('sparringgloves') && !k.endsWith('spatula')
    && !k.endsWith('fryingpan') && !k.endsWith('shimmerscale')
}

// level → (imageUrl → CellPos)
export type LevelBoards = Map<number, Map<string, CellPos>>

export function parseLevelBoards(json: string | null | undefined): LevelBoards {
  if (!json) return new Map()
  try {
    const obj = JSON.parse(json) as Record<string, Record<string, CellPos>>
    const result: LevelBoards = new Map()
    for (const [k, posObj] of Object.entries(obj)) {
      const lv = Number(k)
      if (BOARD_LEVELS.includes(lv))
        result.set(lv, new Map(Object.entries(posObj)))
    }
    return result
  } catch { return new Map() }
}

export function serializeLevelBoards(boards: LevelBoards): string | null {
  const obj: Record<string, Record<string, CellPos>> = {}
  boards.forEach((posMap, lv) => {
    if (posMap.size > 0) obj[String(lv)] = Object.fromEntries(posMap)
  })
  return Object.keys(obj).length > 0 ? JSON.stringify(obj) : null
}

/* ── 배치판 편집 모달 ── */

export function buildKoreanName(deck: AdminDeck, locale: TFTLocale | undefined): string {
  if (!locale) return deck.autoName
  const traitNames = deck.traitSuffixes.slice(0, 2).map((s) => getTraitName(s, locale)).filter(Boolean)
  const carryNames = deck.units
    .filter((u) => u.imageUrl)
    .slice(-2)
    .map((u) => getChampionName(u.imageUrl, locale, ''))
    .filter(Boolean)
  return [...traitNames, ...carryNames].join(' ') || deck.autoName
}
