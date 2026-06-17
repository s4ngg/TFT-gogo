import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { PARTY_RECRUITMENT_ROOM_ID } from '../../../../constants/communityChatRooms'
import type { PartyPost } from '../../types'
import {
  getPartyActionNotice,
  getPartyJoinActionState,
  getPartyListEmptyMessage,
  mergePartyPostSources,
} from '../partyUtils'

function partyPost(id: string): PartyPost {
  return {
    id,
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: '서버 모집글',
    mode: '랭크',
    tier: '마스터+',
    capacity: '1/2',
    close: '마감 예정',
    status: '모집중',
    description: '같이 플레이할 파티원을 구합니다.',
    tags: ['랭크'],
    icon: 'crown',
    tone: 'purple',
  }
}

describe('partyUtils', () => {
  it('서버 쿼리 데이터가 없을 때 더미 모집글을 병합하지 않는다', () => {
    const result = mergePartyPostSources({
      localPosts: [],
      postOverrides: {},
      serverPosts: undefined,
    })

    assert.deepEqual(result, [])
  })

  it('서버 모집글과 로컬 모집글을 중복 없이 병합한다', () => {
    const localPost = partyPost('local-party')
    const serverPost = partyPost('server-party')

    const result = mergePartyPostSources({
      localPosts: [localPost],
      postOverrides: {},
      serverPosts: [serverPost],
    })

    assert.deepEqual(result.map((post) => post.id), ['local-party', 'server-party'])
  })

  it('서버 모집글 배열 내부 중복 ID는 1건만 유지한다', () => {
    const serverPost = partyPost('dup-party')
    const duplicatedServerPost = { ...partyPost('dup-party'), title: '중복 모집글' }

    const result = mergePartyPostSources({
      localPosts: [],
      postOverrides: {},
      serverPosts: [serverPost, duplicatedServerPost],
    })

    assert.deepEqual(result.map((post) => post.id), ['dup-party'])
  })

  it('비로그인 사용자는 파티 액션 로그인 필요 안내를 본다', () => {
    assert.equal(
      getPartyActionNotice(false),
      '로그인이 필요합니다. 모집글 작성과 참여는 로그인 후 사용할 수 있습니다.',
    )
    assert.equal(getPartyActionNotice(true), '')
  })

  it('파티 목록 상태별 빈 문구를 구분한다', () => {
    assert.equal(
      getPartyListEmptyMessage({
        isAuthenticated: false,
        isLoading: true,
        isUnavailable: false,
        selectedFilter: '전체',
      }),
      '파티 모집글을 불러오는 중입니다.',
    )
    assert.equal(
      getPartyListEmptyMessage({
        isAuthenticated: false,
        isLoading: false,
        isUnavailable: true,
        selectedFilter: '전체',
      }),
      '로그인이 필요합니다.',
    )
    assert.equal(
      getPartyListEmptyMessage({
        isAuthenticated: true,
        isLoading: false,
        isUnavailable: false,
        selectedFilter: '전체',
      }),
      '등록된 모집글이 없습니다.',
    )
  })

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
