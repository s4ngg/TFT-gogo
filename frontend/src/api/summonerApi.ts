import axiosInstance from './axiosInstance'
import type { TraitHexBadgeTone } from '../types/badges'
import { mockSummonerProfile } from '../mocks/summonerMock'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// ── Spring이 내려줄 DTO 타입 ─────────────────────────────────

export interface SummonerProfileResponse {
  puuid: string
  gameName: string
  tagLine: string
  profileIconId: number
  summonerLevel: number
  tier: string | null
  rank: string | null
  leaguePoints: number
  wins: number            // 4위 이상(순방) 횟수
  losses: number          // 5~8위(4위 미만) 횟수
}

export interface MatchUnitResponse {
  characterId: string
  imageUrl: string
  stars: 1 | 2 | 3   // ChampionCard는 2|3 기본, 1성도 허용
  itemImageUrls: string[]
}

export interface MatchTraitResponse {
  traitId: string
  name: string
  iconUrl: string
  count: number
  tone: TraitHexBadgeTone
}

export interface MatchParticipantResponse {
  puuid: string
  riotIdGameName: string
  riotIdTagline: string
  placement: number
  stage: string
  traits: MatchTraitResponse[]
  units: MatchUnitResponse[]
  playersEliminated: number
  goldLeft: number
}

export type GameType = 'RANKED' | 'NORMAL'

export interface MatchSummaryResponse {
  matchId: string
  placement: number
  gameDateTime: number    // Unix timestamp ms
  gameType: GameType      // queue_id 기반 Spring 변환값 (1100→RANKED, 1090→NORMAL)
  compositionName: string
  traits: MatchTraitResponse[]
  units: MatchUnitResponse[]
  participants: MatchParticipantResponse[]
}

// ── API 함수 ────────────────────────────────────────────────

export const getSummonerProfile = async (
  gameName: string,
  tagLine: string,
): Promise<SummonerProfileResponse> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<SummonerProfileResponse>>(
      `/summoners/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}`,
      { timeout: 60_000 },
    )
    return data.data
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 404) throw err
    if (status === 429) throw new Error('RATE_LIMITED')
    return mockSummonerProfile
  }
}

export const refreshSummoner = async (
  gameName: string,
  tagLine: string,
): Promise<SummonerProfileResponse> => {
  const { data } = await axiosInstance.post<ApiResponse<SummonerProfileResponse>>(
    `/summoners/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}/refresh`,
    null,
    { timeout: 60_000 },
  )
  return data.data
}

export interface SummonerStatsResponse {
  topTraits: {
    traitId: string
    name: string
    iconUrl: string
    tone: TraitHexBadgeTone
    count: number
    games: number
    avgPlace: number
  }[]
  topChampions: {
    characterId: string
    imageUrl: string
    games: number
    avgPlace: number
  }[]
}

export const getMatchStats = async (puuid: string): Promise<SummonerStatsResponse> => {
  const { data } = await axiosInstance.get<ApiResponse<SummonerStatsResponse>>(
    `/match/${encodeURIComponent(puuid)}/stats`,
    { timeout: 30_000 },
  )
  return data.data
}

export const getMatchHistory = async (
  puuid: string,
  start = 0,
  count = 20,
): Promise<MatchSummaryResponse[]> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<MatchSummaryResponse[]>>(
      `/match/${encodeURIComponent(puuid)}/matches`,
      { params: { start, count }, timeout: 60_000 },
    )
    return data.data
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 404 || status === 204) return []
    throw err
  }
}
