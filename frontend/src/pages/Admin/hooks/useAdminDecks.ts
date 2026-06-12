import { useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAdminDecks, type AdminDeck } from '../../../api/adminApi'
import type { RankFilter } from '../../Dashboard/dashboardData'

const adminDecksQueryKey = (rankFilter: RankFilter) => ['admin', 'decks', rankFilter] as const

export function useAdminDecks(rankFilter: RankFilter) {
  const queryClient = useQueryClient()
  const queryKey = adminDecksQueryKey(rankFilter)
  const decksQuery = useQuery<AdminDeck[]>({
    queryKey,
    queryFn: () => fetchAdminDecks(rankFilter),
    staleTime: 1000 * 60,
  })

  const updateDeck = (updated: AdminDeck) => {
    queryClient.setQueryData<AdminDeck[]>(queryKey, (currentDecks) => {
      if (!currentDecks) {
        return [updated]
      }

      return currentDecks.map((deck) => (deck.id === updated.id ? { ...deck, ...updated } : deck))
    })
  }

  return {
    decks: decksQuery.data ?? [],
    isLoading: decksQuery.isLoading,
    updateDeck,
  }
}
