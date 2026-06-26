import { useEffect, useState } from 'react'
import { adminRefresh, setAccessToken, getAccessToken } from '../api/adminApi'
import { useAdminSession } from './useAdminSession'

type Status = 'loading' | 'authenticated' | 'unauthenticated'

/**
 * 페이지 새로고침 시 in-memory 토큰이 사라지는 경우
 * HttpOnly 쿠키를 이용해 자동으로 Access Token을 복구한다.
 */
export function useAdminAuth(): Status {
  const { setSession, session } = useAdminSession()
  const [status, setStatus] = useState<Status>(() =>
    getAccessToken() ? 'authenticated' : 'loading',
  )

  // interceptor에서 refresh 실패로 clearAdminSession()이 호출되면
  // session이 null로 바뀌고, 이미 authenticated 상태였던 guard를 만료 처리
  useEffect(() => {
    if (status === 'authenticated' && session === null) {
      setStatus('unauthenticated')
    }
  }, [session, status])

  useEffect(() => {
    if (getAccessToken()) {
      setStatus('authenticated')
      return
    }

    let cancelled = false
    adminRefresh()
      .then((newToken) => {
        if (cancelled) return
        setAccessToken(newToken)
        // JWT payload에서 username/role 파싱 (서명 검증 불필요 — 서버가 이미 검증)
        try {
          const payload = JSON.parse(atob(newToken.split('.')[1]))
          setSession({ username: payload.username as string, role: payload.role as string as import('../types/admin').AdminRole })
        } catch {
          // payload 파싱 실패해도 토큰은 유효
        }
        setStatus('authenticated')
      })
      .catch(() => {
        if (!cancelled) setStatus('unauthenticated')
      })

    return () => { cancelled = true }
  }, [setSession])

  return status
}
