import { useInfiniteQuery } from '@tanstack/react-query'
import { getMatchHistory } from '../api/summonerApi'

export const useMatchHistory = (puuid: string, count = 20) =>
  useInfiniteQuery({
    queryKey: ['summoner', 'matches', puuid, count],
    queryFn: ({ pageParam }) => getMatchHistory(puuid, pageParam, count),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length === count ? allPages.length * count : undefined,
    enabled: !!puuid,
    staleTime: 1000 * 60 * 5,
  })
