import axiosInstance from './axiosInstance'
import type { MetaDeck, RankFilter } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export const getMetaDecks = async (rankFilter: RankFilter = 'EMERALD_PLUS'): Promise<MetaDeck[]> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<MetaDeck[]>>('/decks/meta', {
      params: { rankFilter },
    })
    const result = data?.data
    return Array.isArray(result) ? result : []
  } catch {
    return mockDeckMetaResponse
  }
}
