import { PARTY_RECRUITMENT_ROOM_ID } from '../../../constants/communityChatRooms'
import type { ChatRoom } from '../types'

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
