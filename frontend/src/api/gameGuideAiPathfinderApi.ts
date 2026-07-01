import { isAxiosError } from 'axios'
import axiosInstance from './axiosInstance'
import type { GuideEntryType, GuideTab } from './guide'

export type GameGuideAiPathfinderMode = 'AUTO'

export type GameGuideAiPathfinderPhase = 'EARLY' | 'MID' | 'LATE' | 'ANY'

export type GameGuideAiPathfinderErrorCode = 'AUTH_REQUIRED' | 'RATE_LIMITED' | 'REQUEST_FAILED'

export class GameGuideAiPathfinderError extends Error {
  code: GameGuideAiPathfinderErrorCode
  status?: number

  constructor(message: string, code: GameGuideAiPathfinderErrorCode, status?: number) {
    super(message)
    this.name = 'GameGuideAiPathfinderError'
    this.code = code
    this.status = status
  }
}

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
  message?: string
  success: boolean
}

function readApiErrorMessage(error: unknown) {
  if (!isAxiosError<ApiResponse<unknown>>(error)) {
    return undefined
  }

  const message = error.response?.data?.message

  return message?.trim() || undefined
}

function createRequestError(error: unknown) {
  if (isAxiosError<ApiResponse<unknown>>(error)) {
    const status = error.response?.status
    const message = readApiErrorMessage(error)

    if (status === 401) {
      return new GameGuideAiPathfinderError(
        message ?? '로그인이 필요합니다.',
        'AUTH_REQUIRED',
        status,
      )
    }

    if (status === 429) {
      return new GameGuideAiPathfinderError(
        message ?? 'GameGuide AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.',
        'RATE_LIMITED',
        status,
      )
    }
  }

  return new GameGuideAiPathfinderError(
    'GameGuide AI 요청 중 오류가 발생했습니다.',
    'REQUEST_FAILED',
  )
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
      throw new GameGuideAiPathfinderError(
        data.message || 'GameGuide AI 응답을 받지 못했습니다.',
        'REQUEST_FAILED',
      )
    }

    return data.data
  } catch (error) {
    if (error instanceof GameGuideAiPathfinderError) {
      throw error
    }

    throw createRequestError(error)
  }
}
