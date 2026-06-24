import { useQuery } from '@tanstack/react-query'
import { getAiRecommendation, type AiRecommendRequest } from '../api/aiRecommendApi'

export const useAiRecommendQuery = (params: AiRecommendRequest | null, token: string | null) =>
  useQuery({
    queryKey: ['aiRecommendation', params?.gameName, params?.tagLine],
    queryFn: () => getAiRecommendation(params as AiRecommendRequest),
    enabled: !!token && !!params?.gameName && !!params?.tagLine,
    staleTime: 1000 * 60 * 5,
    retry: false,
  })
