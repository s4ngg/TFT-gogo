import { useCallback, useSyncExternalStore } from 'react'
import type { AdminSession } from '../types/admin'

// 모듈 수준의 in-memory 세션 (페이지 새로고침 시 초기화 → refresh cookie로 복구)
let _session: AdminSession | null = null
const _listeners = new Set<() => void>()

function notifyListeners() {
  _listeners.forEach((fn) => fn())
}

export function getAdminSessionSnapshot(): AdminSession | null {
  return _session
}

export function clearAdminSession(): void {
  _session = null
  notifyListeners()
}

function subscribeAdminSession(fn: () => void): () => void {
  _listeners.add(fn)
  return () => _listeners.delete(fn)
}

export function useAdminSession() {
  const session = useSyncExternalStore(
    subscribeAdminSession,
    getAdminSessionSnapshot,
    getAdminSessionSnapshot,
  )

  const setSession = useCallback((newSession: AdminSession | null) => {
    _session = newSession
    notifyListeners()
  }, [])

  return {
    session,
    setSession,
    subscribe: subscribeAdminSession,
  }
}
