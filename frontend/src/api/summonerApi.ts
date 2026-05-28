import axiosInstance from './axiosInstance'
import type { TraitHexBadgeTone } from '../components/common/TraitHexBadge'
import { mockSummonerProfile, mockMatchHistory } from '../mocks/summonerMock'

// ── Spring이 내려줄 DTO 타입 ─────────────────────────────────

export interface SummonerTopTrait {
  traitId: string
  name: string
  count: number
  iconUrl: string
  tone: TraitHexBadgeTone
  games: number
  avgPlace: number
}

export interface SummonerTopChampion {
  characterId: string
  name: string
  imageUrl: string
  cost: number
  games: number
  avgPlace: number
}

export interface SummonerProfileResponse {
  puuid: string
  gameName: string
  tagLine: string
  profileIconId: number
  summonerLevel: number
  tier: string            // 'DIAMOND'
  rank: string            // 'IV'
  leaguePoints: number
  wins: number            // 4위 이상(순방) 횟수
  losses: number          // 5~8위(4위 미만) 횟수
  avgPlace: number
  top4Rate: number
  rankDistribution: number[]   // index 0 = 1등 횟수, ... index 7 = 8등 횟수
  topTraits: SummonerTopTrait[]
  topChampions: SummonerTopChampion[]
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
  augments: string[]
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
  augments: string[]
  participants: MatchParticipantResponse[]
}

// ── API 함수 ────────────────────────────────────────────────

export const getSummonerProfile = async (
  gameName: string,
  tagLine: string,
): Promise<SummonerProfileResponse> => {
  try {
    const { data } = await axiosInstance.get<SummonerProfileResponse>(
      `/summoner/${encodeURIComponent(gameName)}/${tagLine}`,
    )
    return data
  } catch {
    return mockSummonerProfile
  }
}

export const getMatchHistory = async (
  gameName: string,
  tagLine: string,
  count = 90,
): Promise<MatchSummaryResponse[]> => {
  try {
    const { data } = await axiosInstance.get<MatchSummaryResponse[]>(
      `/summoner/${encodeURIComponent(gameName)}/${tagLine}/matches`,
      { params: { count } },
    )
    return data
  } catch {
    return mockMatchHistory
  }
}
