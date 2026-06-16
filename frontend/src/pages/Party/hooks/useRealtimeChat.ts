import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getChatMessages,
  sendChatMessage,
  subscribeChatRoom,
  type ChatMessage,
  type ChatStreamErrorReason,
  type ChatStreamSubscription,
} from '../../../api/chatApi'
import {
  getChatReconnectDelay,
  getNextChatReconnectAttempt,
  MAX_CHAT_RECONNECT_ATTEMPTS,
} from '../utils/chatReconnect'

type ChatConnectionStatus = 'connected' | 'connecting' | 'disconnected'

interface SendMessageParams {
  content: string
  roomId?: string
}

const chatMessagesQueryKey = (roomId: string) => ['chatMessages', roomId] as const

function isTerminalStreamError(reason: ChatStreamErrorReason) {
  return reason === 'client'
}

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

export function useRealtimeChat(roomId: string, streamEnabled = true) {
  const queryClient = useQueryClient()
  const [connectionStatus, setConnectionStatus] = useState<ChatConnectionStatus>('connecting')
  const [errorMessage, setErrorMessage] = useState('')
  const [hasReconnectFailed, setHasReconnectFailed] = useState(false)
  const [isReconnecting, setIsReconnecting] = useState(false)
  const [reconnectAttempt, setReconnectAttempt] = useState(0)

  const messagesQuery = useQuery({
    enabled: Boolean(roomId),
    queryKey: chatMessagesQueryKey(roomId),
    queryFn: () => getChatMessages(roomId),
    staleTime: 10_000,
  })

  useEffect(() => {
    if (!streamEnabled) {
      setConnectionStatus('disconnected')
      setErrorMessage('')
      setHasReconnectFailed(false)
      setIsReconnecting(false)
      setReconnectAttempt(0)
      return undefined
    }

    let active = true
    let activeSubscription: ChatStreamSubscription | null = null
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null
    let currentReconnectAttempt = 0
    let hasOpenedOnce = false

    const clearReconnectTimer = () => {
      if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }

    const closeActiveSubscription = () => {
      if (activeSubscription !== null) {
        activeSubscription.close()
        activeSubscription = null
      }
    }

    const resetReconnectState = () => {
      currentReconnectAttempt = 0
      setHasReconnectFailed(false)
      setIsReconnecting(false)
      setReconnectAttempt(0)
    }

    const stopWithError = (message: string) => {
      clearReconnectTimer()
      closeActiveSubscription()
      setConnectionStatus('disconnected')
      setErrorMessage(message)
      setHasReconnectFailed(true)
      setIsReconnecting(false)
    }

    const connect = () => {
      if (!active) {
        return
      }

      let failureHandled = false

      clearReconnectTimer()
      closeActiveSubscription()
      setConnectionStatus('connecting')

      const handleStreamFailure = (reason: ChatStreamErrorReason | 'close') => {
        if (!active || failureHandled) {
          return
        }

        failureHandled = true
        closeActiveSubscription()

        if (reason !== 'close' && isTerminalStreamError(reason)) {
          stopWithError('채팅방 연결 정보를 확인해주세요.')
          return
        }

        const nextAttempt = getNextChatReconnectAttempt(currentReconnectAttempt)

        if (nextAttempt === null) {
          stopWithError('실시간 연결을 복구하지 못했습니다.')
          return
        }

        const delay = getChatReconnectDelay(nextAttempt)

        if (delay === null) {
          stopWithError('실시간 연결을 복구하지 못했습니다.')
          return
        }

        currentReconnectAttempt = nextAttempt
        setConnectionStatus('connecting')
        setErrorMessage('')
        setHasReconnectFailed(false)
        setIsReconnecting(hasOpenedOnce)
        setReconnectAttempt(nextAttempt)
        reconnectTimer = setTimeout(connect, delay)
      }

      activeSubscription = subscribeChatRoom(roomId, {
        onClose: () => handleStreamFailure('close'),
        onError: (reason) => handleStreamFailure(reason),
        onMessage: (message) => {
          queryClient.setQueryData<ChatMessage[]>(
            chatMessagesQueryKey(message.roomId),
            (currentMessages) => mergeMessages(currentMessages, [message]),
          )
        },
        onOpen: () => {
          if (!active || failureHandled) {
            return
          }

          hasOpenedOnce = true
          resetReconnectState()
          setConnectionStatus('connected')
          setErrorMessage('')
        },
        onSnapshot: (messages) => {
          queryClient.setQueryData<ChatMessage[]>(
            chatMessagesQueryKey(roomId),
            mergeMessages(undefined, messages),
          )
        },
        onUnauthorized: () => {
          if (!active || failureHandled) {
            return
          }

          failureHandled = true
          clearReconnectTimer()
          closeActiveSubscription()
          setConnectionStatus('disconnected')
          setErrorMessage('로그인이 만료되었습니다. 다시 로그인해주세요.')
          setHasReconnectFailed(false)
          setIsReconnecting(false)
          setReconnectAttempt(0)
        },
      })
    }

    resetReconnectState()
    setErrorMessage('')
    connect()

    return () => {
      active = false
      clearReconnectTimer()
      closeActiveSubscription()
    }
  }, [queryClient, roomId, streamEnabled])

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
  }: SendMessageParams) => {
    try {
      return await sendMutation.mutateAsync({
        content,
        roomId: targetRoomId,
      })
    } catch {
      throw new Error('메시지 전송에 실패했습니다.')
    }
  }

  return {
    connectionStatus,
    errorMessage,
    hasReconnectFailed,
    isLoading: messagesQuery.isLoading,
    isReconnecting,
    isSending: sendMutation.isPending,
    maxReconnectAttempts: MAX_CHAT_RECONNECT_ATTEMPTS,
    messages,
    queryError: messagesQuery.error,
    reconnectAttempt,
    sendMessage,
  }
}
