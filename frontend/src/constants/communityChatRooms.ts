export const COMMUNITY_CHAT_ROOM_IDS = [
  'general',
  'deck-guide',
  'party-recruitment',
  'question-answer',
] as const

export type CommunityChatRoomId = (typeof COMMUNITY_CHAT_ROOM_IDS)[number]

export interface CommunityChatRoom {
  id: CommunityChatRoomId
  lastMessage: string
  lastMessageTime: string
  name: string
  users: string
}

export const PARTY_RECRUITMENT_ROOM_ID: CommunityChatRoomId = 'party-recruitment'

export const COMMUNITY_CHAT_ROOMS: readonly CommunityChatRoom[] = [
  { id: 'general', name: '일반', users: '1,234', lastMessage: '새로운 패치 적응 중입니다!', lastMessageTime: '14:58' },
  { id: 'deck-guide', name: '덱 공략', users: '856', lastMessage: '증강 추천 부탁드려요', lastMessageTime: '14:57' },
  { id: PARTY_RECRUITMENT_ROOM_ID, name: '파티 모집', users: '622', lastMessage: '마스터 듀오 구해요~', lastMessageTime: '14:56' },
  { id: 'question-answer', name: '질문 & 답변', users: '741', lastMessage: '초보 운영 질문 있습니다', lastMessageTime: '14:56' },
]

export function createCommunityChatRooms() {
  return COMMUNITY_CHAT_ROOMS.map((room) => ({
    id: room.id,
    lastMessage: room.lastMessage,
    name: room.name,
    users: room.users,
  }))
}
