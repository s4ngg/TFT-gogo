import { PARTY_RECRUITMENT_ROOM_ID } from '../../../constants/communityChatRooms'
import type { ChatMessage } from '../../../api/chatApi'
import type { ChatRoom } from '../types'

const EMPTY_CHAT_PREVIEW = '아직 메시지가 없습니다.'

export function updateChatRoomPreview(rooms: ChatRoom[], roomId: string, lastMessage: string) {
  const normalizedMessage = lastMessage.trim()

  if (!normalizedMessage) {
    return rooms
  }

  return rooms.map((room) =>
    room.id === roomId
      ? {
          ...room,
          lastMessage: normalizedMessage,
        }
      : room,
  )
}

export function updatePartyRecruitmentPreview(rooms: ChatRoom[], lastMessage: string) {
  return updateChatRoomPreview(rooms, PARTY_RECRUITMENT_ROOM_ID, lastMessage)
}

export function applyRoomMessageSnapshot(
  rooms: ChatRoom[],
  roomId: string,
  messages: readonly ChatMessage[],
) {
  const lastMessage = messages[messages.length - 1]
  const uniqueSenderCount = new Set(
    messages
      .map((message) => message.senderName.trim())
      .filter(Boolean),
  ).size

  return rooms.map((room) =>
    room.id === roomId
      ? {
          ...room,
          lastMessage: lastMessage?.content.trim() || EMPTY_CHAT_PREVIEW,
          users: String(uniqueSenderCount),
        }
      : room,
  )
}
