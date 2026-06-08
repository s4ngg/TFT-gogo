import { getChampionApiName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { ChampionSummary } from '../../Dashboard/dashboardData'
import { isFrontline, isBackline } from './champUtils'

export const BOARD_ROWS = 4
export const BOARD_COLS = 7

export interface PlacedChamp {
  champ: ChampionSummary
  row: number
  col: number
  items?: string[]
}

export interface CellPosData {
  row: number
  col: number
  items?: string[]
}

export function placeLine(units: ChampionSummary[], rows: number[], result: PlacedChamp[]) {
  if (units.length === 0) return

  const perRow = Math.ceil(units.length / rows.length)
  let placed = 0

  rows.forEach((row) => {
    const chunk = units.slice(placed, placed + perRow)
    if (chunk.length === 0) return

    const startCol = Math.floor((BOARD_COLS - chunk.length) / 2)
    chunk.forEach((champ, i) => result.push({ champ, row, col: startCol + i }))
    placed += chunk.length
  })
}

export function buildBoardPositions(visible: ChampionSummary[], locale: TFTLocale | undefined): PlacedChamp[] {
  const frontliners = visible.filter((champ) => isFrontline(champ, locale))
  const backliners = visible.filter((champ) => isBackline(champ, locale) && !frontliners.includes(champ))
  const flexUnits = visible.filter((champ) => !frontliners.includes(champ) && !backliners.includes(champ))
  const result: PlacedChamp[] = []

  placeLine(frontliners, [3, 2], result)
  placeLine(flexUnits, [2, 1], result)
  placeLine(backliners, [0, 1], result)

  return result
}

export function parseBoardPositions(json: string | null | undefined, level: number): Map<string, CellPosData> {
  if (!json) return new Map()
  try {
    const obj = JSON.parse(json) as Record<string, unknown>
    const levelData = obj[String(level)]
    if (levelData && typeof levelData === 'object') {
      const entries = Object.entries(levelData as Record<string, unknown>)
        .filter((entry): entry is [string, CellPosData] => {
          const v = entry[1]
          return (
            typeof v === 'object' && v !== null &&
            typeof (v as CellPosData).row === 'number' &&
            typeof (v as CellPosData).col === 'number' &&
            ((v as CellPosData).items === undefined || Array.isArray((v as CellPosData).items))
          )
        })
      return new Map(entries)
    }
    return new Map()
  } catch {
    return new Map()
  }
}

export function buildCustomBoardPositions(
  visibleUnits: ChampionSummary[],
  posMap: Map<string, CellPosData>,
): PlacedChamp[] {
  return visibleUnits.flatMap((champ) => {
    const apiName = getChampionApiName(champ.imageUrl)
    const pos = apiName ? posMap.get(apiName) : undefined
    return pos ? [{ champ, row: pos.row, col: pos.col, items: pos.items }] : []
  })
}
