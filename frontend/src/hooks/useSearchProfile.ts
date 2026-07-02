import { useQuery } from '@tanstack/react-query'
import { getSummonerProfile } from '../api/searchApi'

export const useSearchProfile = (gameName: string, tagLine: string) =>
  useQuery({
    queryKey: ['summoner', 'profile', gameName, tagLine],
    queryFn: () => getSummonerProfile(gameName, tagLine),
    enabled: !!gameName && !!tagLine,
    staleTime: 1000 * 60 * 5,
    retry: false,
  })
