import axiosInstance from './axiosInstance'
import type { TraitHexBadgeTone } from '../types/badges'

export interface AiRecommendRequest {
  gameName: string
  tagLine: string
  recentGameCount?: number
}

export interface AiRecommendStats {
  recentGames: number
  avgPlace: string
  top4Rate: string
  winRate: string
  recentPlacements?: number[]
}

export interface AiRecommendTrait {
  name: string
  count: number
  iconUrl: string
  tone: TraitHexBadgeTone
  games: number
  avgPlace: string
  top4Rate: string
}

export interface AiRecommendDeckReason {
  deckRank: number
  reason: string
  isPatchTrend: boolean
}

export interface AiRecommendResponse {
  stats: AiRecommendStats
  goodTraits: AiRecommendTrait[]
  badTraits: AiRecommendTrait[]
  deckReasons: AiRecommendDeckReason[]
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T | null
}

export const getAiRecommendation = async (params: AiRecommendRequest): Promise<AiRecommendResponse | null> => {
  const { data } = await axiosInstance.get<ApiResponse<AiRecommendResponse>>('/ai/recommend', {
    params,
  })

  return data.data
}
