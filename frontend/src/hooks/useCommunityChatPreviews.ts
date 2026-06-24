import { useQueries } from '@tanstack/react-query'
import { getChatMessages, type ChatMessage } from '../api/chatApi'
import { communityChatMessagesQueryKey } from '../api/chatQueryKeys'
import { COMMUNITY_CHAT_ROOMS, type CommunityChatRoomId } from '../constants/communityChatRooms'

export interface CommunityChatPreview {
  id: CommunityChatRoomId
  message: string
  name: string
  time: string
  users: string
}

function getLatestMessage(messages: ChatMessage[] | undefined) {
  return messages && messages.length > 0 ? messages[messages.length - 1] : undefined
}

function formatMessageTime(createdAt: string) {
  const createdDate = new Date(createdAt)

  if (Number.isNaN(createdDate.getTime())) {
    return '--:--'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(createdDate)
}

export function useCommunityChatPreviews(): CommunityChatPreview[] {
  const messageQueries = useQueries({
    queries: COMMUNITY_CHAT_ROOMS.map((room) => ({
      queryFn: () => getChatMessages(room.id),
      queryKey: communityChatMessagesQueryKey(room.id),
      refetchInterval: 10_000,
      staleTime: 10_000,
    })),
  })

  return COMMUNITY_CHAT_ROOMS.map((room, index) => {
    const latestMessage = getLatestMessage(messageQueries[index]?.data)

    return {
      id: room.id,
      message: latestMessage?.content ?? room.lastMessage,
      name: room.name,
      time: latestMessage ? formatMessageTime(latestMessage.createdAt) : room.lastMessageTime,
      users: room.users,
    }
  })
}
