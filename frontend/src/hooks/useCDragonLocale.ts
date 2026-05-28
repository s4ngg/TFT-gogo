import { useQuery } from '@tanstack/react-query'
import { fetchTFTLocale } from '../api/cdragonLocale'

export function useCDragonLocale() {
  return useQuery({
    queryKey: ['cdragon', 'tft', 'locale', 'ko_kr'],
    queryFn: fetchTFTLocale,
    staleTime: 1000 * 60 * 60 * 24,
    gcTime: 1000 * 60 * 60 * 24,
    retry: 2,
  })
}
