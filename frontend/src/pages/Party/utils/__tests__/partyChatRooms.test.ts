import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { PARTY_RECRUITMENT_ROOM_ID } from '../../../../constants/communityChatRooms'
import type { ChatRoom } from '../../types'
import { updateChatRoomPreview, updatePartyRecruitmentPreview } from '../partyChatRooms'

const fixedRooms: ChatRoom[] = [
  { id: 'general', name: '일반', users: '100', lastMessage: '안녕하세요' },
  { id: 'deck-guide', name: '덱 공략', users: '80', lastMessage: '증강 추천 부탁드려요' },
  { id: PARTY_RECRUITMENT_ROOM_ID, name: '파티 모집', users: '30', lastMessage: '마스터 듀오 구해요~' },
  { id: 'question-answer', name: '질문 & 답변', users: '20', lastMessage: '초보 질문 있습니다' },
]

describe('partyChatRooms', () => {
  it('파티 활동은 고정 파티 모집 채널의 미리보기만 갱신한다', () => {
    const result = updatePartyRecruitmentPreview(fixedRooms, '마스터 듀오 참여 신청했습니다.')

    assert.equal(result.length, 4)
    assert.equal(result[2]?.id, PARTY_RECRUITMENT_ROOM_ID)
    assert.equal(result[2]?.lastMessage, '마스터 듀오 참여 신청했습니다.')
  })

  it('지원하지 않는 채팅방 ID는 새 탭으로 추가하지 않는다', () => {
    const result = updateChatRoomPreview(fixedRooms, 'party-10', '파티별 방이 열렸습니다.')

    assert.equal(result.length, 4)
    assert.equal(result.some((room) => room.id === 'party-10'), false)
    assert.deepEqual(result, fixedRooms)
  })

  it('빈 메시지는 고정 채널 미리보기를 변경하지 않는다', () => {
    const result = updatePartyRecruitmentPreview(fixedRooms, '   ')

    assert.deepEqual(result, fixedRooms)
  })
})
