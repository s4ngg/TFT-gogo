import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'

import { subscribeChatRoom, type ChatStreamErrorReason } from '../chatApi'
import useAuthStore from '../../store/useAuthStore'

const originalFetch = globalThis.fetch

function createHandlers(overrides: Partial<{
  onClose: () => void
  onError: (reason: ChatStreamErrorReason) => void
  onMessage: () => void
  onOpen: () => void
  onSnapshot: () => void
  onUnauthorized: () => void
}> = {}) {
  return {
    onClose: overrides.onClose ?? (() => undefined),
    onError: overrides.onError ?? (() => undefined),
    onMessage: overrides.onMessage ?? (() => undefined),
    onOpen: overrides.onOpen ?? (() => undefined),
    onSnapshot: overrides.onSnapshot ?? (() => undefined),
    onUnauthorized: overrides.onUnauthorized ?? (() => undefined),
  }
}

afterEach(() => {
  globalThis.fetch = originalFetch
  useAuthStore.getState().clearAuth()
})

describe('chatApi subscribeChatRoom', () => {
  it('401 응답은 인증 만료로 처리하고 토큰을 정리한다', async () => {
    useAuthStore.getState().setAuth({ token: 'expired-token' })
    globalThis.fetch = async () => new Response(null, { status: 401 })

    await new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('onUnauthorized was not called')), 1000)

      subscribeChatRoom('general', createHandlers({
        onError: () => {
          clearTimeout(timeout)
          reject(new Error('onError should not be called for 401'))
        },
        onUnauthorized: () => {
          clearTimeout(timeout)
          assert.equal(useAuthStore.getState().token, null)
          resolve()
        },
      }))
    })
  })

  it('403 응답은 공개 SSE의 client 오류로 처리하고 토큰을 유지한다', async () => {
    useAuthStore.getState().setAuth({ token: 'valid-token' })
    globalThis.fetch = async () => new Response(null, { status: 403 })

    await new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('onError was not called')), 1000)

      subscribeChatRoom('general', createHandlers({
        onError: (reason) => {
          clearTimeout(timeout)
          assert.equal(reason, 'client')
          assert.equal(useAuthStore.getState().token, 'valid-token')
          resolve()
        },
        onUnauthorized: () => {
          clearTimeout(timeout)
          reject(new Error('onUnauthorized should not be called for 403'))
        },
      }))
    })
  })
})
