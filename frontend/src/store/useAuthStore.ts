import { create } from 'zustand'
import useSummonerStore from './useSummonerStore'

interface AuthPayload {
  token: string
}

interface AuthState {
  token: string | null
  setAuth: (auth: AuthPayload) => void
  clearAuth: () => void
}

function removeLegacyPersistedAuth() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem('tftgogo-auth')
}

removeLegacyPersistedAuth()

const useAuthStore = create<AuthState>()((set) => ({
  token: null,
  setAuth: ({ token }) => {
    removeLegacyPersistedAuth()
    set({ token })
  },
  clearAuth: () => {
    removeLegacyPersistedAuth()
    set({ token: null })
    useSummonerStore.getState().clearSummoner()
  },
}))

export default useAuthStore
