import { useState, useCallback } from 'react'
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

export function useAdminSession() {
  const [, rerender] = useState(0)

  const subscribe = useCallback((fn: () => void) => {
    _listeners.add(fn)
    return () => _listeners.delete(fn)
  }, [])

  const setSession = useCallback((session: AdminSession | null) => {
    _session = session
    notifyListeners()
    rerender((n) => n + 1)
  }, [])

  return {
    session: _session,
    setSession,
    subscribe,
  }
}
