import { useQuery } from '@tanstack/react-query'
import { getMe } from '../api/memberApi'
import useAuthStore from '../store/useAuthStore'

export const AUTH_ME_QUERY_KEY = ['auth', 'me'] as const

export function useAuthSession() {
  const token = useAuthStore((state) => state.token)

  return useQuery({
    enabled: Boolean(token),
    queryFn: getMe,
    queryKey: AUTH_ME_QUERY_KEY,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}
