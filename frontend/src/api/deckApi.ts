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
    return data.data ?? []
  } catch {
    return mockDeckMetaResponse
  }
}
