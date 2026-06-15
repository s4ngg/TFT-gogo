import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  COMMUNITY_CHAT_ROOM_IDS,
  DEFAULT_COMMUNITY_CHAT_ROOM_ID,
  normalizeCommunityChatRoomId,
} from '../communityChatRooms'

describe('communityChatRooms', () => {
  it('지원하는 고정 채팅방 ID를 그대로 반환한다', () => {
    COMMUNITY_CHAT_ROOM_IDS.forEach((roomId) => {
      assert.equal(normalizeCommunityChatRoomId(roomId), roomId)
    })
  })

  it('앞뒤 공백을 제거한 뒤 고정 채팅방 ID를 반환한다', () => {
    assert.equal(normalizeCommunityChatRoomId(' party-recruitment '), 'party-recruitment')
  })

  it('비어 있거나 지원하지 않는 채팅방 ID는 null을 반환한다', () => {
    const invalidRoomIds = ['', '   ', null, undefined, 'party-10', '../../x']

    invalidRoomIds.forEach((roomId) => {
      assert.equal(normalizeCommunityChatRoomId(roomId), null)
    })
  })

  it('기본 채팅방은 일반 채널이다', () => {
    assert.equal(DEFAULT_COMMUNITY_CHAT_ROOM_ID, 'general')
  })
})
