import { useQuery } from '@tanstack/react-query'
import { getAiRecommendation, type AiRecommendRequest } from '../api/aiRecommendApi'

export const useAiRecommendQuery = (params: AiRecommendRequest | null) =>
  useQuery({
    queryKey: ['aiRecommendation', params?.gameName, params?.tagLine, params?.recentGameCount],
    queryFn: () => getAiRecommendation(params as AiRecommendRequest),
    enabled: !!params?.gameName && !!params?.tagLine,
    staleTime: 1000 * 60 * 5,
  })
