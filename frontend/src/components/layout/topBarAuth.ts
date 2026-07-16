import { AUTH_ME_QUERY_KEY } from '../../hooks/useAuthSession'
import { setLogoutInProgress } from '../../api/authSessionControl'

export interface TopBarAuthQueryClient {
  cancelQueries: (filters: { queryKey: readonly unknown[]; exact?: boolean }) => Promise<unknown>
  removeQueries: (filters: { queryKey: readonly unknown[]; exact?: boolean }) => void
}

export async function clearTopBarAuthSession(
  queryClient: TopBarAuthQueryClient,
  clearAuth: () => void,
  logoutRequest: (accessToken?: string) => Promise<void> = async () => undefined,
  accessToken?: string | null,
): Promise<void> {
  setLogoutInProgress(true)

  clearAuth()

  try {
    await logoutRequest(accessToken ?? undefined)
  } catch {
    // Server logout failure must not keep the local session visible.
  } finally {
    setLogoutInProgress(false)
  }

  await Promise.allSettled([
    queryClient.cancelQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true }),
    queryClient.cancelQueries({ queryKey: ['aiRecommendation'] }),
  ])

  queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
  queryClient.removeQueries({ queryKey: ['aiRecommendation'] })
}
