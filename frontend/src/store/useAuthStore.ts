import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface AuthUser {
  email: string
  id?: number | string
  nickname?: string
  profileIconId?: number
  summonerName?: string
  tagLine?: string
  tier?: string
}

interface AuthPayload {
  token: string
}

interface AuthState {
  token: string | null
  setAuth: (auth: AuthPayload) => void
  clearAuth: () => void
}

function hasToken(value: unknown): value is { token?: string | null } {
  return typeof value === 'object' && value !== null && 'token' in value
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      setAuth: ({ token }) => set({ token }),
      clearAuth: () => set({ token: null }),
    }),
    {
      name: 'tftgogo-auth',
      version: 1,
      migrate: (persistedState) => ({
        token: hasToken(persistedState) ? persistedState.token ?? null : null,
      }),
      partialize: (state) => ({
        token: state.token,
      }),
    },
  ),
)

export default useAuthStore
