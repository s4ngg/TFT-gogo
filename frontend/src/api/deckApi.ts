import axiosInstance from './axiosInstance'
import type { MetaDeck } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

export const getMetaDecks = async (): Promise<MetaDeck[]> => {
  try {
    const { data } = await axiosInstance.get<MetaDeck[]>('/decks/meta')
    return data
  } catch {
    return mockDeckMetaResponse
  }
}
