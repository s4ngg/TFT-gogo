import { useQuery } from '@tanstack/react-query'
import type { HeroAugmentDeckItem } from '../api/adminApi'

// TODO: 공개 API 엔드포인트 연결 후 교체 예정
export const useHeroAugmentDecks = () =>
  useQuery<HeroAugmentDeckItem[]>({
    queryKey: ['decks', 'hero-augment'],
    queryFn: async () => [],
    staleTime: 1000 * 60 * 5,
  })
