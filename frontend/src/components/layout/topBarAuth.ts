import { AUTH_ME_QUERY_KEY } from '../../hooks/useAuthSession'
import { setLogoutInProgress } from '../../api/authSessionControl'

interface TopBarAuthQueryClient {
  cancelQueries: (filters: { queryKey: readonly string[]; exact?: boolean }) => Promise<unknown>
  removeQueries: (filters: { queryKey: readonly string[]; exact?: boolean }) => void
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

  try {
    await queryClient.cancelQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
    await queryClient.cancelQueries({ queryKey: ['aiRecommendation'] })
  } catch {
    // Cache cancellation must not keep a locally logged-in session alive.
  }

  queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY, exact: true })
  queryClient.removeQueries({ queryKey: ['aiRecommendation'] })
}
