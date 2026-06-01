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

export interface AiRecommendAugment {
  name: string
  avgPlace: string
  games: number
  icon: string
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
  augments: AiRecommendAugment[]
  deckReasons: AiRecommendDeckReason[]
}

export const getAiRecommendation = async (params: AiRecommendRequest) => {
  const { data } = await axiosInstance.get<AiRecommendResponse>('/ai/recommendations', {
    params,
  })

  return data
}
