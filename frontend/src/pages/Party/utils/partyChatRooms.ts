import type { ChatRoom, PartyPost } from '../types'

const CHAT_ROOM_ID_PREFIX = 'party-'

export function createPartyChatRoomId(postId: string) {
  return postId.startsWith(CHAT_ROOM_ID_PREFIX) ? postId : `${CHAT_ROOM_ID_PREFIX}${postId}`
}

export function createPartyChatRoom(post: PartyPost, lastMessage?: string): ChatRoom {
  return {
    id: post.chatRoomId,
    lastMessage: lastMessage ?? `${post.title} 전용 채팅방이 열렸습니다.`,
    name: post.title,
    users: post.capacity,
  }
}

export function upsertChatRoom(rooms: ChatRoom[], room: ChatRoom) {
  const roomIndex = rooms.findIndex((currentRoom) => currentRoom.id === room.id)

  if (roomIndex < 0) {
    return [room, ...rooms]
  }

  return rooms.map((currentRoom, index) =>
    index === roomIndex
      ? {
          ...currentRoom,
          lastMessage: room.lastMessage,
          name: room.name,
          users: room.users,
        }
      : currentRoom,
  )
}
