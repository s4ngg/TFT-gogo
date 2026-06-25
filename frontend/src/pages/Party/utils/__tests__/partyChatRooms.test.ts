import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { PARTY_RECRUITMENT_ROOM_ID } from '../../../../constants/communityChatRooms'
import type { ChatMessage } from '../../../../api/chatApi'
import type { ChatRoom } from '../../types'
import {
  applyRoomMessageSnapshot,
  updateChatRoomPreview,
  updatePartyRecruitmentPreview,
} from '../partyChatRooms'

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
    const unsupportedRoomId = 'party-10' as string
    const result = updateChatRoomPreview(fixedRooms, unsupportedRoomId, '파티별 방이 열렸습니다.')

    assert.equal(result.length, 4)
    assert.equal(result.some((room) => room.id === unsupportedRoomId), false)
    assert.deepEqual(result, fixedRooms)
  })

  it('빈 메시지는 고정 채널 미리보기를 변경하지 않는다', () => {
    const result = updatePartyRecruitmentPreview(fixedRooms, '   ')

    assert.deepEqual(result, fixedRooms)
  })

  it('채팅방 미리보기는 최근 메시지와 고유 작성자 수로 갱신한다', () => {
    const messages: ChatMessage[] = [
      {
        id: 'message-1',
        roomId: 'general',
        senderId: 1,
        senderName: '소정',
        tier: 'Unranked',
        content: '첫 메시지',
        createdAt: '2026-06-22T10:00:00',
      },
      {
        id: 'message-2',
        roomId: 'general',
        senderId: 2,
        senderName: '상우',
        tier: 'Unranked',
        content: '마지막 메시지',
        createdAt: '2026-06-22T10:01:00',
      },
      {
        id: 'message-3',
        roomId: 'general',
        senderId: 1,
        senderName: '소정',
        tier: 'Unranked',
        content: '다시 참여',
        createdAt: '2026-06-22T10:02:00',
      },
    ]

    const result = applyRoomMessageSnapshot(fixedRooms, 'general', messages)

    assert.equal(result[0]?.lastMessage, '다시 참여')
    assert.equal(result[0]?.users, '2')
  })

  it('메시지가 없으면 고정 채널 인원수를 0으로 표시한다', () => {
    const result = applyRoomMessageSnapshot(fixedRooms, 'general', [])

    assert.equal(result[0]?.lastMessage, '아직 메시지가 없습니다.')
    assert.equal(result[0]?.users, '0')
  })

  it('지원하지 않는 채팅방 스냅샷은 새 탭을 만들지 않는다', () => {
    const unsupportedRoomId = 'party-10' as string
    const messages: ChatMessage[] = [
      {
        id: 'message-1',
        roomId: unsupportedRoomId,
        senderId: 1,
        senderName: '소정',
        tier: 'Unranked',
        content: '파티별 방이 열렸습니다.',
        createdAt: '2026-06-22T10:00:00',
      },
    ]

    const result = applyRoomMessageSnapshot(fixedRooms, unsupportedRoomId, messages)

    assert.equal(result.length, 4)
    assert.equal(result.some((room) => room.id === unsupportedRoomId), false)
    assert.deepEqual(result, fixedRooms)
  })
})
