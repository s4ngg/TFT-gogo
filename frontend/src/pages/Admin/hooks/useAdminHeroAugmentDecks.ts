import { useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAdminHeroAugmentDecks, type HeroAugmentDeckItem } from '../../../api/adminApi'

const adminHeroAugmentDecksQueryKey = ['admin', 'hero-augment-decks'] as const

export function useAdminHeroAugmentDecks() {
  const queryClient = useQueryClient()
  const decksQuery = useQuery<HeroAugmentDeckItem[]>({
    queryKey: adminHeroAugmentDecksQueryKey,
    queryFn: fetchAdminHeroAugmentDecks,
    staleTime: 1000 * 60,
  })

  const removeDeck = (id: number) => {
    queryClient.setQueryData<HeroAugmentDeckItem[]>(adminHeroAugmentDecksQueryKey, (currentDecks) =>
      currentDecks?.filter((deck) => deck.id !== id) ?? [],
    )
  }

  const upsertDeck = (updatedDeck: HeroAugmentDeckItem) => {
    queryClient.setQueryData<HeroAugmentDeckItem[]>(adminHeroAugmentDecksQueryKey, (currentDecks) => {
      if (!currentDecks) {
        return [updatedDeck]
      }

      const exists = currentDecks.some((deck) => deck.id === updatedDeck.id)
      return exists
        ? currentDecks.map((deck) => (deck.id === updatedDeck.id ? updatedDeck : deck))
        : [...currentDecks, updatedDeck]
    })
  }

  return {
    decks: decksQuery.data ?? [],
    error: decksQuery.error,
    isError: decksQuery.isError,
    isLoading: decksQuery.isLoading,
    removeDeck,
    refetch: decksQuery.refetch,
    upsertDeck,
  }
}
