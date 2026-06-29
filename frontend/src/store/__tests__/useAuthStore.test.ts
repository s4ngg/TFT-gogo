import assert from 'node:assert/strict'
import { beforeEach, describe, it } from 'node:test'

import useAuthStore from '../useAuthStore'
import useSummonerStore from '../useSummonerStore'

describe('useAuthStore.clearAuth', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'test-token' })
    useSummonerStore.setState({
      summoner: { name: '테스터', tag: 'KR1', tier: 'GOLD', lp: 100, wins: 10, losses: 5, emblemKey: 'gold' },
    })
  })

  it('clearAuth 호출 시 token이 null이 된다', () => {
    useAuthStore.getState().clearAuth()
    assert.equal(useAuthStore.getState().token, null)
  })

  it('clearAuth 호출 시 summoner도 null이 된다', () => {
    useAuthStore.getState().clearAuth()
    assert.equal(useSummonerStore.getState().summoner, null)
  })
})
