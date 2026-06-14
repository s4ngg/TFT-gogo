import { useQuery } from '@tanstack/react-query'
import { getMatchStats } from '../api/summonerApi'

export const useMatchStats = (puuid: string) =>
  useQuery({
    queryKey: ['summoner', 'stats', puuid],
    queryFn: () => getMatchStats(puuid),
    enabled: !!puuid,
    staleTime: 1000 * 60 * 5,
  })
