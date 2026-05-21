import axiosInstance from './axiosInstance'

const RIOT_ENDPOINTS = {
  summonerByName: '/tft/summoner/v1/summoners/by-name',
  matchIdsByPuuid: '/tft/match/v1/matches/by-puuid',
  matchDetail: '/tft/match/v1/matches',
  leagueBySummoner: '/tft/league/v1/entries/by-summoner',
} as const

export type RiotEndpointKey = keyof typeof RIOT_ENDPOINTS

export function getRiotEndpoint(key: RiotEndpointKey) {
  return RIOT_ENDPOINTS[key]
}

export function getSummonerByName(summonerName: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.summonerByName}/${summonerName}`)
}

export function getMatchIdsByPuuid(puuid: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.matchIdsByPuuid}/${puuid}/ids`)
}

export function getMatchDetail(matchId: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.matchDetail}/${matchId}`)
}

export function getLeagueBySummoner(encryptedSummonerId: string) {
  return axiosInstance.get(`${RIOT_ENDPOINTS.leagueBySummoner}/${encryptedSummonerId}`)
}

export { RIOT_ENDPOINTS }
