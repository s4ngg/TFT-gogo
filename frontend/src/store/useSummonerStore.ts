import { create } from 'zustand'

export interface SummonerRank {
  name: string
  tag: string
  tier: string
  lp: number
  wins: number
  losses: number
  emblemKey: string
}

interface SummonerState {
  summoner: SummonerRank | null
  setSummoner: (summoner: SummonerRank) => void
  clearSummoner: () => void
}

const useSummonerStore = create<SummonerState>((set) => ({
  summoner: null,
  setSummoner: (summoner) => set({ summoner }),
  clearSummoner: () => set({ summoner: null }),
}))

export default useSummonerStore
