import axiosInstance from './axiosInstance'
import type { GuideEntryType, GuideTab } from './guide'

export type GameGuideAiPathfinderMode = 'AUTO'

export type GameGuideAiPathfinderPhase = 'EARLY' | 'MID' | 'LATE' | 'ANY'

export interface GameGuideAiPathfinderRef {
  guideType: GuideEntryType
  name?: string
  targetKey: string
}

export interface GameGuideAiConversationMessage {
  content: string
  role: 'assistant' | 'user'
}

export interface GameGuideAiPathfinderRequest {
  activeTab: GuideTab
  candidateRefs: GameGuideAiPathfinderRef[]
  conversationHistory: GameGuideAiConversationMessage[]
  mode: GameGuideAiPathfinderMode
  patchVersion: string
  question: string
  selectedRefs: GameGuideAiPathfinderRef[]
}

export interface GameGuideAiPathfinderPhasePlan {
  description: string
  guideRefs: GameGuideAiPathfinderRef[]
  phase: GameGuideAiPathfinderPhase
  title: string
}

export interface GameGuideAiPathfinderRecommendedRef extends GameGuideAiPathfinderRef {
  reason: string
}

export interface GameGuideAiPathfinderResponse {
  avoidMistakes: string[]
  coreConcepts: string[]
  creativeSuggestions: string[]
  evidenceNotes: string[]
  isFallback: boolean
  limitations: string[]
  phasePlan: GameGuideAiPathfinderPhasePlan[]
  recommendedRefs: GameGuideAiPathfinderRecommendedRef[]
  sourceRefs: GameGuideAiPathfinderRef[]
  summary: string
  title: string
}

interface ApiResponse<T> {
  data: T | null
  message: string
  success: boolean
}

export const requestGameGuideAiPathfinder = async (
  request: GameGuideAiPathfinderRequest,
): Promise<GameGuideAiPathfinderResponse> => {
  try {
    const { data } = await axiosInstance.post<ApiResponse<GameGuideAiPathfinderResponse>>(
      '/ai/gameguide-pathfinder',
      request,
      { timeout: 35000 },
    )

    if (!data.success || !data.data) {
      throw new Error(data.message || 'GameGuide AI 응답을 받지 못했습니다.')
    }

    return data.data
  } catch {
    throw new Error('GameGuide AI 요청 중 오류가 발생했습니다.')
  }
}
