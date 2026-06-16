import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { PARTY_RECRUITMENT_ROOM_ID } from '../../constants/communityChatRooms'
import { communityChatMessagesQueryKey } from '../chatQueryKeys'

describe('communityChatMessagesQueryKey', () => {
  it('대시보드와 파티 화면이 같은 채팅 메시지 캐시 키를 사용한다', () => {
    const roomIdFromApi: string = PARTY_RECRUITMENT_ROOM_ID

    assert.deepEqual(
      communityChatMessagesQueryKey(PARTY_RECRUITMENT_ROOM_ID),
      communityChatMessagesQueryKey(roomIdFromApi),
    )
    assert.deepEqual(
      communityChatMessagesQueryKey(PARTY_RECRUITMENT_ROOM_ID),
      ['community', 'chat', 'messages', 'party-recruitment'],
    )
  })
})
