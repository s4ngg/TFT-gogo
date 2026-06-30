import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { isLogoutInProgress } from '../api/authSessionControl'
import { getMe, refreshSession } from '../api/memberApi'
import useAuthStore from '../store/useAuthStore'

export const AUTH_ME_QUERY_KEY = ['auth', 'me'] as const

async function getAuthenticatedMember() {
  try {
    if (isLogoutInProgress()) {
      throw new Error('Restore auth session skipped during logout.')
    }

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
  const token = useAuthStore((state) => state.token)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (token) {
      void queryClient.invalidateQueries({ queryKey: AUTH_ME_QUERY_KEY })
      return
    }

    queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
  }, [queryClient, token])

  return useQuery({
    queryFn: getAuthenticatedMember,
    queryKey: AUTH_ME_QUERY_KEY,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}
