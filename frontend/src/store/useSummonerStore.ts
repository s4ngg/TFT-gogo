import { create } from 'zustand'
import { persist } from 'zustand/middleware'

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

const useSummonerStore = create<SummonerState>()(
  persist(
    (set) => ({
      summoner: null,
      setSummoner: (summoner) => set({ summoner }),
      clearSummoner: () => set({ summoner: null }),
    }),
    {
      name: 'tftgogo-summoner',
      version: 1,
      partialize: (state) => ({ summoner: state.summoner }),
    },
  ),
)

export default useSummonerStore
