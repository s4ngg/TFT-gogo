import { AUTH_ME_QUERY_KEY } from '../../hooks/useAuthSession'

const AI_RECOMMEND_QUERY_KEY = ['aiRecommendation'] as const

interface TopBarAuthQueryClient {
  cancelQueries: (filters: { queryKey: typeof AUTH_ME_QUERY_KEY; exact: true }) => Promise<unknown>
  removeQueries: (filters: { queryKey: string[]; exact?: boolean }) => void
}

export async function clearTopBarAuthSession(
  queryClient: TopBarAuthQueryClient,
  clearAuth: () => void,
): Promise<void> {
  clearAuth()

  try {
    await queryClient.cancelQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
  } catch {
    // Cache cancellation must not keep a locally logged-in session alive.
  }

  queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
  queryClient.removeQueries({ queryKey: [...AI_RECOMMEND_QUERY_KEY] })
}
