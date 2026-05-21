import { create } from 'zustand'

export interface SummonerRank {
  name: string
  tag: string
  tier: string    // '플래티넘 II'
  tierKey: string // 'platinum' — for color mapping
  lp: number
  wins: number
  losses: number
}

const MAX_RECENT = 5
const STORAGE_KEY = 'tft-recent-searches'

function loadRecent(): string[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

interface SummonerState {
  summoner: SummonerRank | null
  recentSearches: string[]
  setSummoner: (summoner: SummonerRank) => void
  clearSummoner: () => void
  addRecentSearch: (query: string) => void
}

const useSummonerStore = create<SummonerState>((set) => ({
  summoner: null,
  recentSearches: loadRecent(),
  setSummoner: (summoner) => set({ summoner }),
  clearSummoner: () => set({ summoner: null }),
  addRecentSearch: (query) =>
    set((state) => {
      const next = [query, ...state.recentSearches.filter((q) => q !== query)].slice(0, MAX_RECENT)
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)) } catch { /* noop */ }
      return { recentSearches: next }
    }),
}))

export default useSummonerStore
