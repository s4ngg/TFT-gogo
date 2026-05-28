import axiosInstance from './axiosInstance'
import type { MetaDeck } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const getMetaDecks = async (): Promise<MetaDeck[]> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<MetaDeck[]>>('/decks/meta')
    if (!data.success) {
      throw new Error(data.message ?? '메타 덱 조회 실패')
    }
    return Array.isArray(data.data) ? data.data : []
  } catch {
    return mockDeckMetaResponse
  }
}
