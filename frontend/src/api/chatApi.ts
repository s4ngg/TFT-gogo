import axiosInstance from './axiosInstance'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatContext {
  summonerName?: string
  tagLine?: string
  statsSummary?: string
  goodTraits?: string[]
  badTraits?: string[]
}

export interface ChatRequest {
  messages: ChatMessage[]
  context?: ChatContext
}

export interface ChatResponse {
  reply: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T | null
}

export const sendChatMessage = async (request: ChatRequest): Promise<string> => {
  const { data } = await axiosInstance.post<ApiResponse<ChatResponse>>('/ai/chat', request, {
    timeout: 35000,
  })

  if (!data.data) {
    throw new Error(data.message || 'AI 응답을 받지 못했습니다.')
  }

  return data.data.reply
}
