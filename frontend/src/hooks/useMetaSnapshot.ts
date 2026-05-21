import { useQuery } from '@tanstack/react-query'
import { metaDecks, type MetaDeck } from '../pages/Dashboard/dashboardData'

export function useMetaSnapshot() {
  return useQuery<MetaDeck[]>({
    queryKey: ['metaSnapshot'],
    queryFn: async () => metaDecks,
    staleTime: 1000 * 60 * 3,
  })
}
