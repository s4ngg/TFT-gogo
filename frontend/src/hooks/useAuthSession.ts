import { useQuery } from '@tanstack/react-query'
import { getMe, refreshSession } from '../api/memberApi'
import useAuthStore from '../store/useAuthStore'

export const AUTH_ME_QUERY_KEY = ['auth', 'me'] as const

async function getAuthenticatedMember() {
  try {
    if (!useAuthStore.getState().token) {
      await refreshSession()
    }

    return getMe()
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Restore auth session failed: ${message}`)
  }
}

export function useAuthSession() {
  useAuthStore((state) => state.token)

  return useQuery({
    queryFn: getAuthenticatedMember,
    queryKey: AUTH_ME_QUERY_KEY,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}
