import axiosInstance from './axiosInstance'

const RIOT_ENDPOINTS = {
  accountByRiotId: '/riot/account/v1/accounts/by-riot-id',
  summonerByPuuid: '/tft/summoner/v1/summoners/by-puuid',
  leagueByPuuid: '/tft/league/v1/by-puuid',
  matchIdsByPuuid: '/tft/match/v1/matches/by-puuid',
  matchDetail: '/tft/match/v1/matches',
  apiStatus: '/riot/status',
} as const

export type RiotEndpointKey = keyof typeof RIOT_ENDPOINTS

interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

export type RiotApiStatusKind = 'available' | 'queue' | 'limited' | 'degraded'

export interface RiotApiStatusResponse {
  activeConnections: number
  checkedAt: string
  message: string
  queueSize: number
  status: RiotApiStatusKind
}

export function getRiotEndpoint(key: RiotEndpointKey) {
  return RIOT_ENDPOINTS[key]
}

export function getAccountByRiotId(gameName: string, tagLine: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.accountByRiotId}/${gameName}/${tagLine}`)
}

export function getSummonerByPuuid(puuid: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.summonerByPuuid}/${puuid}`)
}

export function getLeagueByPuuid(puuid: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.leagueByPuuid}/${puuid}`)
}

export function getMatchIdsByPuuid(puuid: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.matchIdsByPuuid}/${puuid}/ids`)
}

export function getMatchDetail(matchId: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.matchDetail}/${matchId}`)
}

export async function getRiotApiStatus() {
  const { data } = await axiosInstance.get<ApiResponse<RiotApiStatusResponse>>(RIOT_ENDPOINTS.apiStatus)

  if (!data.success) {
    throw new Error(data.message ?? 'Riot API 상태 조회 실패')
  }

  return data.data
}

export { RIOT_ENDPOINTS }
