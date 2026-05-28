import { useQuery } from '@tanstack/react-query'
import { getMetaDecks } from '../api/deckApi'
import type { MetaDeckListResponse } from '../api/deckApi'
import type { RankFilter } from '../pages/Dashboard/dashboardData'

export const useDeckQuery = (rankFilter: RankFilter = 'EMERALD_PLUS') =>
  useQuery<MetaDeckListResponse>({
    queryKey: ['decks', 'meta', rankFilter],
    queryFn: () => getMetaDecks(rankFilter),
    staleTime: 1000 * 60 * 5,
  })
