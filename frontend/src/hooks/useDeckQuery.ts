import { useQuery } from '@tanstack/react-query'
import { getMetaDecks } from '../api/deckApi'
import type { MetaDeck } from '../pages/Dashboard/dashboardData'

export const useDeckQuery = () =>
  useQuery<MetaDeck[]>({
    queryKey: ['decks', 'meta'],
    queryFn: getMetaDecks,
    staleTime: 1000 * 60 * 5,
  })
