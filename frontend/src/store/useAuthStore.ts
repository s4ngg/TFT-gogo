import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import useSummonerStore from './useSummonerStore'

interface AuthPayload {
  token: string
}

interface AuthState {
  token: string | null
  setAuth: (auth: AuthPayload) => void
  clearAuth: () => void
}

function hasToken(value: unknown): value is { token: string | null } {
  if (typeof value !== 'object' || value === null || !('token' in value)) {
    return false
  }

  const token = (value as { token?: unknown }).token

  return typeof token === 'string' || token === null
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      setAuth: ({ token }) => set({ token }),
      clearAuth: () => {
        set({ token: null })
        useSummonerStore.getState().clearSummoner()
      },
    }),
    {
      name: 'tftgogo-auth',
      version: 1,
      migrate: (persistedState) => ({
        token: hasToken(persistedState) ? persistedState.token : null,
      }),
      partialize: (state) => ({
        token: state.token,
      }),
    },
  ),
)

export default useAuthStore
