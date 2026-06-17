import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getMe } from '../../api/memberApi'
import { AUTH_ME_QUERY_KEY } from '../../hooks/useAuthSession'
import useAuthStore from '../../store/useAuthStore'
import styles from './AuthPage.module.css'
import { parseSocialAuthCallback, readSocialAuthErrorCode } from './utils/socialAuthCallback'

function OAuthCallbackPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const setAuth = useAuthStore((state) => state.setAuth)
  const clearAuth = useAuthStore((state) => state.clearAuth)
  const handledRef = useRef(false)

  useEffect(() => {
    if (handledRef.current) {
      return
    }
    handledRef.current = true

    async function completeSocialLogin() {
      try {
        const { token } = parseSocialAuthCallback({
          hash: location.hash,
          search: location.search,
        })

        window.history.replaceState(null, '', '/oauth/callback')
        queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY })
        setAuth({ token })
        const member = await queryClient.fetchQuery({
          queryFn: getMe,
          queryKey: AUTH_ME_QUERY_KEY,
          staleTime: 0,
        })

        queryClient.setQueryData(AUTH_ME_QUERY_KEY, member)
        navigate('/', { replace: true })
      } catch {
        clearAuth()
        queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY })
        const oauthError = readSocialAuthErrorCode(location.search)
        navigate(`/login?oauthError=${oauthError}`, { replace: true })
      }
    }

    void completeSocialLogin()
  }, [clearAuth, location.hash, location.search, navigate, queryClient, setAuth])

  return (
    <main className={styles.callbackStatus} aria-label="소셜 로그인 처리 중">
      <section className={`${styles.formPanel} ${styles.callbackPanel}`} role="status" aria-live="polite">
        <span className={styles.callbackSpinner} aria-hidden="true" />
        <h1 className={styles.callbackTitle}>소셜 로그인 처리 중</h1>
        <p className={styles.callbackText}>계정 정보를 확인하고 있습니다.</p>
      </section>
    </main>
  )
}

export default OAuthCallbackPage
