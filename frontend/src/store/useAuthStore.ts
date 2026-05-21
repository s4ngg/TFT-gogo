import { create } from 'zustand'

interface AuthState {
  user: unknown
  setUser: (user: unknown) => void
  clearUser: () => void
}

const useAuthStore = create<AuthState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  clearUser: () => set({ user: null }),
}))

export default useAuthStore
