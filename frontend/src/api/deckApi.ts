import axiosInstance from './axiosInstance'
import type { MetaDeck, RankFilter } from '../pages/Dashboard/dashboardData'

/**
 * 중복 덱 제거: 동일 시너지 조합(traitGroup) 중 avgPlacement 최선 1개만 노출
 * 표본 수 필터는 백엔드(MIN_SAMPLE=10)에 위임 — 프론트에서 재적용하지 않음
 */
function deduplicateDecks(decks: MetaDeck[]): MetaDeck[] {
  const toAvg = (d: MetaDeck): number => {
    const n = parseFloat(d.avgPlace)
    return Number.isFinite(n) ? n : Infinity
  }
  const grouped = new Map<string, MetaDeck>()
  for (const deck of decks) {
    const traitGroup = deck.traits
      .map((t) => t.name)
      .sort()
      .join('|')
    const existing = grouped.get(traitGroup)
    if (!existing || toAvg(deck) < toAvg(existing)) grouped.set(traitGroup, deck)
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

export const getMetaDecks = async (rankFilter: RankFilter = 'EMERALD_PLUS'): Promise<MetaDeckListResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<MetaDeckListResponse>>('/decks/meta', {
    params: { rankFilter },
  })

  if (!data.success) {
    throw new Error(data.message ?? '메타 덱 조회 실패')
  }

  if (!data.data || !Array.isArray(data.data.decks)) {
    throw new Error('메타 덱 응답 형식 오류')
  }

  return {
    patchVersion: data.data.patchVersion ?? null,
    rankFilter: data.data.rankFilter ?? rankFilter,
    dataStartDate: data.data.dataStartDate ?? null,
    decks: deduplicateDecks(data.data.decks),
  }
}
