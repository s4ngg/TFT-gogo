import axiosInstance from './axiosInstance'

export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AiChatContext {
  summonerName?: string
  tagLine?: string
  statsSummary?: string
  goodTraits?: string[]
  badTraits?: string[]
  recentMatches?: string
  topChampions?: string[]
}

export interface AiChatRequest {
  messages: AiChatMessage[]
  context?: AiChatContext
}

export interface AiChatResponse {
  reply: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T | null
}

export const sendAiChatMessage = async (request: AiChatRequest): Promise<string> => {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AiChatResponse>>('/ai/chat', request, {
      timeout: 35000,
    })

    if (!data.success || !data.data) {
      throw new Error(data.message || 'AI 응답을 받지 못했습니다.')
    }

    return data.data.reply
  } catch {
    throw new Error('AI 요청 중 오류가 발생했습니다.')
  }
}
