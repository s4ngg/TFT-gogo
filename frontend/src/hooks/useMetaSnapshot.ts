import { useDeckQuery } from './useDeckQuery'
import type { RankFilter } from '../pages/Dashboard/dashboardData'

export function useMetaSnapshot(rankFilter?: RankFilter) {
  return useDeckQuery(rankFilter)
}
