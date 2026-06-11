import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getChatMessages,
  sendChatMessage,
  subscribeChatRoom,
  type ChatMessage,
} from '../../../api/chatApi'

type ChatConnectionStatus = 'connected' | 'connecting' | 'disconnected'

interface SendMessageParams {
  content: string
  roomId?: string
  senderName: string
  tier?: string
}

const chatMessagesQueryKey = (roomId: string) => ['chatMessages', roomId] as const

function mergeMessages(currentMessages: ChatMessage[] | undefined, nextMessages: ChatMessage[]) {
  const messageMap = new Map<string, ChatMessage>()

  currentMessages?.forEach((message) => {
    messageMap.set(message.id, message)
  })
  nextMessages.forEach((message) => {
    messageMap.set(message.id, message)
  })

  return Array.from(messageMap.values()).sort((left, right) =>
    left.createdAt.localeCompare(right.createdAt),
  )
}

export function useRealtimeChat(roomId: string, enabled = true) {
  const queryClient = useQueryClient()
  const [connectionStatus, setConnectionStatus] = useState<ChatConnectionStatus>('connecting')
  const [errorMessage, setErrorMessage] = useState('')

  const messagesQuery = useQuery({
    enabled,
    queryKey: chatMessagesQueryKey(roomId),
    queryFn: () => getChatMessages(roomId),
    staleTime: 10_000,
  })

  useEffect(() => {
    if (!enabled) {
      setConnectionStatus('disconnected')
      return undefined
    }

    let active = true

    setConnectionStatus('connecting')
    const subscription = subscribeChatRoom(roomId, {
      onError: () => {
        if (active) {
          setConnectionStatus('disconnected')
        }
      },
      onMessage: (message) => {
        queryClient.setQueryData<ChatMessage[]>(
          chatMessagesQueryKey(message.roomId),
          (currentMessages) => mergeMessages(currentMessages, [message]),
        )
      },
      onOpen: () => {
        if (active) {
          setConnectionStatus('connected')
        }
      },
      onSnapshot: (messages) => {
        queryClient.setQueryData<ChatMessage[]>(
          chatMessagesQueryKey(roomId),
          (currentMessages) => mergeMessages(currentMessages, messages),
        )
      },
    })

    return () => {
      active = false
      subscription.close()
    }
  }, [enabled, queryClient, roomId])

  const sendMutation = useMutation({
    mutationFn: sendChatMessage,
    onError: () => {
      setErrorMessage('메시지 전송에 실패했습니다.')
    },
    onSuccess: (message) => {
      setErrorMessage('')
      queryClient.setQueryData<ChatMessage[]>(
        chatMessagesQueryKey(message.roomId),
        (currentMessages) => mergeMessages(currentMessages, [message]),
      )
    },
  })

  const messages = useMemo(
    () => messagesQuery.data ?? [],
    [messagesQuery.data],
  )

  const sendMessage = async ({
    content,
    roomId: targetRoomId = roomId,
    senderName,
    tier,
  }: SendMessageParams) => {
    try {
      return await sendMutation.mutateAsync({
        content,
        roomId: targetRoomId,
        senderName,
        tier,
      })
    } catch {
      throw new Error('메시지 전송에 실패했습니다.')
    }
  }

  return {
    connectionStatus,
    errorMessage,
    isLoading: messagesQuery.isLoading,
    isSending: sendMutation.isPending,
    messages,
    queryError: messagesQuery.error,
    sendMessage,
  }
}
