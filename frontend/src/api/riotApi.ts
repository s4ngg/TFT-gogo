import axiosInstance from './axiosInstance'

const RIOT_ENDPOINTS = {
  accountByRiotId: '/riot/account/v1/accounts/by-riot-id',
  summonerByPuuid: '/tft/summoner/v1/summoners/by-puuid',
  leagueByPuuid: '/tft/league/v1/by-puuid',
  matchIdsByPuuid: '/tft/match/v1/matches/by-puuid',
  matchDetail: '/tft/match/v1/matches',
} as const

export type RiotEndpointKey = keyof typeof RIOT_ENDPOINTS

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

export { RIOT_ENDPOINTS }
