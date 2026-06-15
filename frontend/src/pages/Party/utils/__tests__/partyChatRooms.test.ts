import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import type { PartyPost } from '../../types'
import {
  createPartyChatRoom,
  createPartyChatRoomId,
  syncPartyChatRooms,
  upsertChatRoom,
} from '../partyChatRooms'

const partyPost: PartyPost = {
  capacity: '1/2',
  chatRoomId: 'party-10',
  close: '방금 등록',
  description: '저녁 랭크 같이 하실 분 구합니다.',
  icon: 'crown',
  id: '10',
  mode: '랭크',
  status: '모집중',
  tags: ['음성 가능'],
  tier: '마스터+',
  title: '마스터 듀오 구합니다',
  tone: 'purple',
}

describe('partyChatRooms', () => {
  it('post id로 안전한 파티 전용 채팅방 ID를 만든다', () => {
    assert.equal(createPartyChatRoomId('10'), 'party-10')
    assert.equal(createPartyChatRoomId('party-local'), 'party-local')
  })

  it('파티 모집글로 전용 채팅방 항목을 만든다', () => {
    const room = createPartyChatRoom(partyPost, '참여했습니다.')

    assert.deepEqual(room, {
      id: 'party-10',
      lastMessage: '참여했습니다.',
      name: '마스터 듀오 구합니다',
      users: '1/2',
    })
  })

  it('기존 채팅방은 중복 추가하지 않고 최신 정보로 갱신한다', () => {
    const rooms = [
      { id: 'general', name: '일반', users: '100', lastMessage: '안녕하세요' },
      { id: 'party-10', name: '이전 제목', users: '1/2', lastMessage: '이전 메시지' },
    ]

    const result = upsertChatRoom(rooms, createPartyChatRoom(partyPost, '새 메시지'))

    assert.equal(result.length, 2)
    assert.deepEqual(result[1], {
      id: 'party-10',
      lastMessage: '새 메시지',
      name: '마스터 듀오 구합니다',
      users: '1/2',
    })
  })

  it('서버 파티 목록으로 전용 채팅방을 복원한다', () => {
    const rooms = [
      { id: 'general', name: '일반', users: '100', lastMessage: '안녕하세요' },
    ]

    const result = syncPartyChatRooms(rooms, [partyPost])

    assert.equal(result.length, 2)
    assert.deepEqual(result[0], {
      id: 'party-10',
      lastMessage: '마스터 듀오 구합니다 전용 채팅방이 열렸습니다.',
      name: '마스터 듀오 구합니다',
      users: '1/2',
    })
    assert.deepEqual(result[1], rooms[0])
  })

  it('동일한 서버 파티를 반복 동기화해도 채팅방을 중복 추가하지 않는다', () => {
    const rooms = [
      { id: 'general', name: '일반', users: '100', lastMessage: '안녕하세요' },
    ]
    const syncedRooms = syncPartyChatRooms(rooms, [partyPost])
    const result = syncPartyChatRooms(syncedRooms, [partyPost])

    assert.equal(result.filter((room) => room.id === 'party-10').length, 1)
    assert.equal(result.length, 2)
  })

  it('복원된 기존 파티방은 마지막 메시지를 보존하고 제목과 인원만 갱신한다', () => {
    const rooms = [
      {
        id: 'party-10',
        name: '이전 제목',
        users: '1/2',
        lastMessage: '방금 보낸 메시지',
      },
    ]
    const updatedPost: PartyPost = {
      ...partyPost,
      capacity: '2/2',
      title: '마감된 마스터 듀오',
    }

    const result = syncPartyChatRooms(rooms, [updatedPost])

    assert.deepEqual(result, [
      {
        id: 'party-10',
        lastMessage: '방금 보낸 메시지',
        name: '마감된 마스터 듀오',
        users: '2/2',
      },
    ])
  })
})
