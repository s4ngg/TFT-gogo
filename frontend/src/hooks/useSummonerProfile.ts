import { useQuery } from '@tanstack/react-query'
import { getSummonerProfile } from '../api/summonerApi'

export const useSummonerProfile = (gameName: string, tagLine: string) =>
  useQuery({
    queryKey: ['summoner', 'profile', gameName, tagLine],
    queryFn: () => getSummonerProfile(gameName, tagLine),
    enabled: !!gameName && !!tagLine,
    staleTime: 1000 * 60 * 5,
  })
