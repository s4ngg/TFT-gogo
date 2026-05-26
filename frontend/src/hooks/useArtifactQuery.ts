import { useQuery } from '@tanstack/react-query'
import { getArtifactRecs } from '../api/deckListApi'

export const useArtifactQuery = () =>
  useQuery({
    queryKey: ['decks', 'artifacts'],
    queryFn: getArtifactRecs,
    staleTime: 1000 * 60 * 5,
  })
