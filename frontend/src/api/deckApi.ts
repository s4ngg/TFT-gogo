import axiosInstance from './axiosInstance'
import type { MetaDeck, RankFilter } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

const MIN_SAMPLE_SIZE = 20

/**
 * 중복 덱 제거 (A+B)
 * A. sampleSize < MIN_SAMPLE_SIZE 인 덱 제외
 * B. 동일 시너지 조합(traitGroup) 중 avgPlacement 최선 1개만 노출
 */
function deduplicateDecks(decks: MetaDeck[]): MetaDeck[] {
  // A: 표본 수 필터 (sampleSize가 정의된 경우만 적용)
  const filtered = decks.filter(
    (d) => d.sampleSize === undefined || d.sampleSize >= MIN_SAMPLE_SIZE,
  )

  // B: 같은 traitGroup → avgPlacement(낮을수록 좋음) 최선 1개만
  const grouped = new Map<string, MetaDeck>()
  for (const deck of filtered) {
    const traitGroup = deck.traits
      .map((t) => t.name)
      .sort()
      .join('|')
    const existing = grouped.get(traitGroup)
    const deckAvg  = parseFloat(deck.avgPlace)
    const existAvg = existing ? parseFloat(existing.avgPlace) : Infinity
    if (!existing || deckAvg < existAvg) grouped.set(traitGroup, deck)
  }

  return Array.from(grouped.values()).sort((a, b) => a.rank - b.rank)
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface MetaDeckListResponse {
  patchVersion: string | null
  rankFilter: RankFilter
  dataStartDate: string | null
  decks: MetaDeck[]
}

const fallbackMetaDeckListResponse: MetaDeckListResponse = {
  patchVersion: '17.3',
  rankFilter: 'EMERALD_PLUS',
  dataStartDate: null,
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
      dataStartDate: data.data?.dataStartDate ?? null,
      decks: deduplicateDecks(Array.isArray(data.data?.decks) ? data.data.decks : []),
    }
  } catch {
    return { ...fallbackMetaDeckListResponse, rankFilter }
  }
}
