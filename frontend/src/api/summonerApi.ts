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
  tier: string
  rank: string
  leaguePoints: number
  wins: number
  losses: number
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
    )
    return data.data
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 404) throw err
    return mockSummonerProfile
  }
}

export const getMatchHistory = async (
  gameName: string,
  tagLine: string,
  count = 90,
): Promise<MatchSummaryResponse[]> => {
  try {
    const { data } = await axiosInstance.get<ApiResponse<MatchSummaryResponse[]>>(
      `/summoners/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}/matches`,
      { params: { count }, timeout: 120000 },
    )
    return data.data
  } catch {
    return []
  }
}
