import { useQuery } from '@tanstack/react-query'
import { fetchAdminHeroAugmentDecks, type HeroAugmentDeckItem } from '../api/adminApi'

export function useHeroAugmentDecks() {
  return useQuery<HeroAugmentDeckItem[]>({
    queryKey: ['decks', 'hero-augment'],
    queryFn: fetchAdminHeroAugmentDecks,
    staleTime: 1000 * 60 * 5,
  })
}
