import axiosInstance from './axiosInstance'
import type { MetaDeck, RankFilter } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface MetaDeckListResponse {
  patchVersion: string | null
  rankFilter: RankFilter
  decks: MetaDeck[]
}

const fallbackMetaDeckListResponse: MetaDeckListResponse = {
  patchVersion: '17.3',
  rankFilter: 'EMERALD_PLUS',
  decks: mockDeckMetaResponse,
}

export const getMetaDecks = async (rankFilter: RankFilter = 'EMERALD_PLUS'): Promise<MetaDeckListResponse> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<MetaDeckListResponse>>('/decks/meta', {
      params: { rankFilter },
    })

    if (!data.success) {
      throw new Error(data.message ?? '메타 덱 조회 실패')
    }

    return {
      patchVersion: data.data?.patchVersion ?? null,
      rankFilter: data.data?.rankFilter ?? rankFilter,
      decks: Array.isArray(data.data?.decks) ? data.data.decks : [],
    }
  } catch {
    return { ...fallbackMetaDeckListResponse, rankFilter }
  }
}
