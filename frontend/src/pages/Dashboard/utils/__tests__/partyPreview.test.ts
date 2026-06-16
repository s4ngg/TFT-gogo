import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { PARTY_RECRUITMENT_ROOM_ID } from '../../../../constants/communityChatRooms'
import type { PartyMode, PartyPost } from '../../../../api/partyApi'
import {
  getPartyPreviewStatusLabel,
  selectPartyPreviewPosts,
} from '../partyPreview'

function partyPost(id: string, mode: PartyMode): PartyPost {
  return {
    id,
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: `${mode} 모집글`,
    mode,
    tier: '마스터+',
    capacity: '1/2',
    close: '마감 예정',
    status: '모집중',
    description: '같이 플레이할 파티원을 구합니다.',
    tags: ['음성 가능'],
    icon: 'crown',
    tone: 'purple',
  }
}

describe('partyPreview', () => {
  it('전체 필터는 기본 4개까지만 반환한다', () => {
    const posts = [
      partyPost('rank-1', '랭크'),
      partyPost('normal-1', '일반'),
      partyPost('custom-1', '커스텀'),
      partyPost('rank-2', '랭크'),
      partyPost('rank-3', '랭크'),
    ]

    const result = selectPartyPreviewPosts(posts, '전체')

    assert.deepEqual(result.map((post) => post.id), ['rank-1', 'normal-1', 'custom-1', 'rank-2'])
  })

  it('선택한 모드만 필터링한다', () => {
    const posts = [
      partyPost('rank-1', '랭크'),
      partyPost('normal-1', '일반'),
      partyPost('custom-1', '커스텀'),
    ]

    const result = selectPartyPreviewPosts(posts, '일반')

    assert.deepEqual(result.map((post) => post.id), ['normal-1'])
  })

  it('전달한 limit을 적용한다', () => {
    const posts = [
      partyPost('rank-1', '랭크'),
      partyPost('rank-2', '랭크'),
      partyPost('rank-3', '랭크'),
    ]

    const result = selectPartyPreviewPosts(posts, '랭크', 2)

    assert.deepEqual(result.map((post) => post.id), ['rank-1', 'rank-2'])
  })

  it('원본 배열을 변경하지 않는다', () => {
    const posts = [partyPost('rank-1', '랭크'), partyPost('normal-1', '일반')]

    const result = selectPartyPreviewPosts(posts, '랭크')

    assert.notEqual(result, posts)
    assert.deepEqual(posts.map((post) => post.id), ['rank-1', 'normal-1'])
  })

  it('대기중 상태는 대시보드에서 마감으로 표시한다', () => {
    assert.equal(getPartyPreviewStatusLabel('대기중'), '마감')
    assert.equal(getPartyPreviewStatusLabel('모집중'), '모집중')
  })
})
