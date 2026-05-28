import axiosInstance from './axiosInstance'
import type { MetaDeck } from '../pages/Dashboard/dashboardData'
import { mockDeckMetaResponse } from '../mocks/deckResponseMock'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isMetaDeck(value: unknown): value is MetaDeck {
  if (!isRecord(value)) return false

  return (
    typeof value.rank === 'number' &&
    typeof value.grade === 'string' &&
    typeof value.name === 'string' &&
    typeof value.winRate === 'string' &&
    typeof value.top4 === 'string' &&
    typeof value.avgPlace === 'string' &&
    typeof value.pickRate === 'string' &&
    Array.isArray(value.traits) &&
    Array.isArray(value.champions)
  )
}

function parseMetaDeckResponse(data: unknown): MetaDeck[] | null {
  if (Array.isArray(data) && data.every(isMetaDeck)) {
    return data
  }

  if (isRecord(data) && Array.isArray(data.data) && data.data.every(isMetaDeck)) {
    return data.data
  }

  return null
}

export const getMetaDecks = async (): Promise<MetaDeck[]> => {
  try {
    const { data } = await axiosInstance.get<unknown>('/decks/meta')
    return parseMetaDeckResponse(data) ?? mockDeckMetaResponse
  } catch {
    return mockDeckMetaResponse
  }
}
