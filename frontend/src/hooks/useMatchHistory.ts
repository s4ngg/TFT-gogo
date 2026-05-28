import { useQuery } from '@tanstack/react-query'
import { getMatchHistory } from '../api/summonerApi'

export const useMatchHistory = (gameName: string, tagLine: string, count = 90) =>
  useQuery({
    queryKey: ['summoner', 'matches', gameName, tagLine, count],
    queryFn: () => getMatchHistory(gameName, tagLine, count),
    enabled: !!gameName && !!tagLine,
    staleTime: 1000 * 60 * 5,
  })
