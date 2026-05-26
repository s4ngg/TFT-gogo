import { useQuery } from '@tanstack/react-query'
import { getHeroAugments } from '../api/deckListApi'

export const useHeroAugmentQuery = () =>
  useQuery({
    queryKey: ['decks', 'hero-augments'],
    queryFn: getHeroAugments,
    staleTime: 1000 * 60 * 5,
  })
