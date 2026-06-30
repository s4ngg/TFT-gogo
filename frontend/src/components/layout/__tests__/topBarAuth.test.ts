import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { AUTH_ME_QUERY_KEY } from '../../../hooks/useAuthSession'
import { clearTopBarAuthSession } from '../topBarAuth'

interface QueryCall {
  exact?: boolean
  queryKey: readonly unknown[]
}

function createQueryClient(options: { rejectCancel?: boolean } = {}) {
  const calls: string[] = []
  const cancelFilters: QueryCall[] = []
  const removeFilters: QueryCall[] = []

  return {
    calls,
    client: {
      cancelQueries: async (filters: QueryCall) => {
        calls.push('cancel')
        cancelFilters.push(filters)

        if (options.rejectCancel) {
          throw new Error('cancel failed')
        }
      },
      removeQueries: (filters: QueryCall) => {
        calls.push('remove')
        removeFilters.push(filters)
      },
    },
    cancelFilters,
    removeFilters,
  }
}

describe('clearTopBarAuthSession', () => {
  it('토큰을 먼저 지우고 auth/me 쿼리를 취소한 뒤 제거한다', async () => {
    const queryClient = createQueryClient()
    const calls: string[] = []

    await clearTopBarAuthSession(queryClient.client, () => {
      calls.push('clearAuth')
      queryClient.calls.push('clearAuth')
    })

    assert.deepEqual(queryClient.calls, ['clearAuth', 'cancel', 'cancel', 'remove', 'remove'])
    assert.deepEqual(queryClient.cancelFilters[0], {
      exact: true,
      queryKey: AUTH_ME_QUERY_KEY,
    })
    assert.deepEqual(queryClient.cancelFilters[1], {
      queryKey: ['aiRecommendation'],
    })
    assert.deepEqual(queryClient.removeFilters[0], {
      exact: true,
      queryKey: AUTH_ME_QUERY_KEY,
    })
    assert.deepEqual(queryClient.removeFilters[1], {
      queryKey: ['aiRecommendation'],
    })
    assert.deepEqual(calls, ['clearAuth'])
  })

  it('로컬 토큰을 지운 뒤에도 캡처한 access token으로 서버 로그아웃을 요청한다', async () => {
    const queryClient = createQueryClient()
    const calls: string[] = []
    let logoutToken: string | undefined

    await clearTopBarAuthSession(
      queryClient.client,
      () => {
        calls.push('clearAuth')
        queryClient.calls.push('clearAuth')
      },
      async (accessToken) => {
        calls.push('logout')
        logoutToken = accessToken
      },
      'captured-access-token',
    )

    assert.equal(logoutToken, 'captured-access-token')
    assert.deepEqual(calls, ['clearAuth', 'logout'])
    assert.deepEqual(queryClient.calls, ['clearAuth', 'cancel', 'cancel', 'remove', 'remove'])
  })

  it('쿼리 취소가 실패해도 로컬 세션과 캐시 제거 흐름을 유지한다', async () => {
    const queryClient = createQueryClient({ rejectCancel: true })
    let clearAuthCount = 0

    await clearTopBarAuthSession(queryClient.client, () => {
      clearAuthCount += 1
      queryClient.calls.push('clearAuth')
    })

    assert.equal(clearAuthCount, 1)
    assert.deepEqual(queryClient.calls, ['clearAuth', 'cancel', 'cancel', 'remove', 'remove'])
    assert.deepEqual(queryClient.removeFilters[0], {
      exact: true,
      queryKey: AUTH_ME_QUERY_KEY,
    })
  })
})
