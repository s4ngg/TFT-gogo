import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { getPartyJoinActionState } from '../partyUtils'

describe('partyUtils', () => {
  it('비로그인 사용자는 참여 버튼을 누를 수 없고 로그인 안내를 본다', () => {
    const result = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: false,
      isFull: false,
      isJoined: false,
      isJoinPending: false,
      isOwner: false,
    })

    assert.equal(result.disabled, true)
    assert.equal(result.label, '로그인 후 참여')
  })

  it('참여 중인 모집글은 마감 상태여도 취소할 수 있다', () => {
    const result = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: true,
      isFull: true,
      isJoined: true,
      isJoinPending: false,
      isOwner: false,
    })

    assert.equal(result.disabled, false)
    assert.equal(result.label, '참여중')
  })

  it('비로그인 상태에서는 stale 참여 상태가 남아 있어도 버튼을 열지 않는다', () => {
    const result = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: false,
      isFull: false,
      isJoined: true,
      isJoinPending: false,
      isOwner: false,
    })

    assert.deepEqual(result, { disabled: true, label: '로그인 후 참여' })
  })

  it('작성자와 처리중 상태는 참여 토글을 막는다', () => {
    const ownerResult = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: true,
      isFull: false,
      isJoined: true,
      isJoinPending: false,
      isOwner: true,
    })
    const pendingResult = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: true,
      isFull: false,
      isJoined: false,
      isJoinPending: true,
      isOwner: false,
    })

    assert.deepEqual(ownerResult, { disabled: true, label: '작성자' })
    assert.deepEqual(pendingResult, { disabled: true, label: '처리중' })
  })

  it('로그인 사용자는 참여 가능한 모집글에서 참여 버튼을 본다', () => {
    const result = getPartyJoinActionState({
      hasJoinedOtherPost: false,
      isAuthenticated: true,
      isFull: false,
      isJoined: false,
      isJoinPending: false,
      isOwner: false,
    })

    assert.equal(result.disabled, false)
    assert.equal(result.label, '참여')
  })
})
