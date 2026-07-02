import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface SearchRank {
  name: string
  tag: string
  tier: string
  lp: number
  wins: number
  losses: number
  emblemKey: string
}

interface SearchState {
  summoner: SearchRank | null
  setSummoner: (summoner: SearchRank) => void
  clearSummoner: () => void
}

const useSearchStore = create<SearchState>()(
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

export default useSearchStore
