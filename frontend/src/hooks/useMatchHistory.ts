import { useQuery } from '@tanstack/react-query'
import { getMatchHistory } from '../api/summonerApi'

export const useMatchHistory = (gameName: string, tagLine: string) =>
  useQuery({
    queryKey: ['summoner', 'matches', gameName, tagLine],
    queryFn: () => getMatchHistory(gameName, tagLine, 0),
    enabled: !!gameName && !!tagLine,
    staleTime: 1000 * 60 * 5,
  })
