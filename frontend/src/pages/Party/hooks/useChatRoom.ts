import { useCallback, useEffect, useMemo } from 'react'
import { useMutation, useQueries, useQueryClient } from '@tanstack/react-query'
import {
  buildChatStreamUrl,
  getChatMessages,
  parseChatStreamMessage,
  sendChatMessage,
  type ChatMessage,
} from '../../../api/chatApi'
import useAuthStore from '../../../store/useAuthStore'

export type ChatRoomId = 'general' | 'deck-guide' | 'party-recruitment' | 'qna'

export const chatMessageQueryKey = (roomId: string) => ['chatMessages', roomId] as const

interface SendMessageInput {
  message: string
  roomId: string
}

function appendMessage(currentMessages: ChatMessage[] | undefined, nextMessage: ChatMessage) {
  const messages = currentMessages ?? []

  if (messages.some((message) => message.id === nextMessage.id)) {
    return messages
  }

  return [...messages, nextMessage]
}

export function useChatRoom(activeRoomId: ChatRoomId, roomIds: ChatRoomId[]) {
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const currentSenderName = user?.nickname ?? user?.summonerName ?? '익명'
  const currentSenderTier = user?.tier ?? 'Unranked'

  const messageQueries = useQueries({
    queries: roomIds.map((roomId) => ({
      queryKey: chatMessageQueryKey(roomId),
      queryFn: () => getChatMessages(roomId),
      staleTime: 30_000,
    })),
  })

  const messagesByRoom = useMemo(
    () =>
      roomIds.reduce<Record<ChatRoomId, ChatMessage[]>>(
        (accumulator, roomId, index) => ({
          ...accumulator,
          [roomId]: messageQueries[index]?.data ?? [],
        }),
        {
          general: [],
          'deck-guide': [],
          'party-recruitment': [],
          qna: [],
        },
      ),
    [messageQueries, roomIds],
  )

  const sendMutation = useMutation({
    mutationFn: ({ roomId, message }: SendMessageInput) =>
      sendChatMessage(roomId, {
        message,
        senderName: currentSenderName,
        senderTier: currentSenderTier,
      }),
    onSuccess: (message) => {
      queryClient.setQueryData<ChatMessage[]>(
        chatMessageQueryKey(message.roomId),
        (currentMessages) => appendMessage(currentMessages, message),
      )
    },
  })

  const sendMessage = useCallback(
    (message: string) => sendMutation.mutateAsync({ roomId: activeRoomId, message }),
    [activeRoomId, sendMutation],
  )

  useEffect(() => {
    const source = new EventSource(buildChatStreamUrl(activeRoomId), {
      withCredentials: true,
    })

    source.addEventListener('message', (event) => {
      let parsedMessage: ChatMessage | null = null

      try {
        parsedMessage = parseChatStreamMessage(JSON.parse(event.data) as unknown)
      } catch {
        parsedMessage = null
      }

      if (!parsedMessage) {
        return
      }

      queryClient.setQueryData<ChatMessage[]>(
        chatMessageQueryKey(parsedMessage.roomId),
        (currentMessages) => appendMessage(currentMessages, parsedMessage),
      )
    })

    return () => {
      source.close()
    }
  }, [activeRoomId, queryClient])

  return {
    activeMessages: messagesByRoom[activeRoomId],
    currentSenderName,
    isLoading: messageQueries.some((query) => query.isLoading),
    isSending: sendMutation.isPending,
    messagesByRoom,
    sendError: sendMutation.error,
    sendMessage,
  }
}
