import { useQuery } from '@tanstack/react-query'
import axiosInstance from '../api/axiosInstance'
import type { HeroAugmentDeckItem } from '../api/adminApi'

async function fetchHeroAugmentDecks(): Promise<HeroAugmentDeckItem[]> {
  const { data } = await axiosInstance.get('/hero-augment-decks')
  return data.data
}

export function useHeroAugmentDecks() {
  return useQuery({
    queryKey: ['heroAugmentDecks'],
    queryFn: fetchHeroAugmentDecks,
    staleTime: 1000 * 60 * 5,
  })
}
