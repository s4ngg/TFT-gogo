export const COMMUNITY_CHAT_ROOM_IDS = [
  'general',
  'deck-guide',
  'party-recruitment',
  'question-answer',
] as const

export type CommunityChatRoomId = (typeof COMMUNITY_CHAT_ROOM_IDS)[number]

export const DEFAULT_COMMUNITY_CHAT_ROOM_ID: CommunityChatRoomId = 'general'
export const COMMUNITY_CHAT_ROOM_QUERY_PARAM = 'room'

export interface CommunityChatRoom {
  id: CommunityChatRoomId
  lastMessage: string
  lastMessageTime: string
  name: string
  users: string
}

export interface CommunityChatRoomPreview {
  id: CommunityChatRoomId
  lastMessage: string
  name: string
  users: string
}

export const PARTY_RECRUITMENT_ROOM_ID: CommunityChatRoomId = 'party-recruitment'

export const COMMUNITY_CHAT_ROOMS: readonly CommunityChatRoom[] = [
  { id: 'general', name: '일반', users: '0', lastMessage: '아직 메시지가 없습니다.', lastMessageTime: '--:--' },
  { id: 'deck-guide', name: '덱 공략', users: '0', lastMessage: '아직 메시지가 없습니다.', lastMessageTime: '--:--' },
  { id: PARTY_RECRUITMENT_ROOM_ID, name: '파티 모집', users: '0', lastMessage: '아직 메시지가 없습니다.', lastMessageTime: '--:--' },
  { id: 'question-answer', name: '질문 & 답변', users: '0', lastMessage: '아직 메시지가 없습니다.', lastMessageTime: '--:--' },
]

export function createCommunityChatRooms(): CommunityChatRoomPreview[] {
  return COMMUNITY_CHAT_ROOMS.map((room) => ({
    id: room.id,
    lastMessage: room.lastMessage,
    name: room.name,
    users: room.users,
  }))
}

export function normalizeCommunityChatRoomId(
  value: string | null | undefined,
): CommunityChatRoomId | null {
  const normalizedValue = value?.trim()

  if (!normalizedValue) {
    return null
  }

  return COMMUNITY_CHAT_ROOM_IDS.find((roomId) => roomId === normalizedValue) ?? null
}
