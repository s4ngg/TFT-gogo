import { useQuery } from '@tanstack/react-query'
import type { HeroAugmentDeckItem } from '../../../api/adminApi'

// TODO: 공개 API 엔드포인트 연결 후 교체 예정
// /decks 공개 화면에서 사용하는 hook — admin API 직접 호출 금지 (401 발생)
export function useHeroAugmentDecks() {
  return useQuery<HeroAugmentDeckItem[]>({
    queryKey: ['decks', 'hero-augment'],
    queryFn: async () => [],
    staleTime: 1000 * 60 * 5,
  })
}
