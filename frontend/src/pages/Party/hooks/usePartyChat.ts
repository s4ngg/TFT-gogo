import { useEffect, useMemo, useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { getChatMessages } from '../../../api/chatApi'
import { communityChatMessagesQueryKey } from '../../../api/chatQueryKeys'
import {
  COMMUNITY_CHAT_ROOM_IDS,
  PARTY_RECRUITMENT_ROOM_ID,
} from '../../../constants/communityChatRooms'
import type { CommunityChatRoomId } from '../../../constants/communityChatRooms'
import { initialChatRooms } from '../data/partyMockData'
import {
  applyRoomMessageSnapshot,
  updateChatRoomPreview,
} from '../utils/partyChatRooms'
import { usePartyAuth } from './usePartyAuth'
import { useRealtimeChat } from './useRealtimeChat'

const MAX_CHAT_MESSAGE_LENGTH = 500

interface UsePartyChatOptions {
  activeRoomId: CommunityChatRoomId
  onActiveRoomChange: (roomId: CommunityChatRoomId) => void
}

export function usePartyChat({ activeRoomId, onActiveRoomChange }: UsePartyChatOptions) {
  const { isAuthenticated, userId: currentUserId } = usePartyAuth()
  const [rooms, setRooms] = useState(initialChatRooms)
  const [chatInput, setChatInput] = useState('')
  const [chatStatusMessage, setChatStatusMessage] = useState('')
  const roomPreviewQueries = useQueries({
    queries: COMMUNITY_CHAT_ROOM_IDS.map((roomId) => ({
      queryFn: () => getChatMessages(roomId),
      queryKey: communityChatMessagesQueryKey(roomId),
      staleTime: 10_000,
    })),
  })
  const {
    connectionStatus,
    errorMessage: chatErrorMessage,
    hasReconnectFailed,
    isLoading,
    isReconnecting,
    isSending,
    maxReconnectAttempts,
    messages: activeMessages,
    queryError,
    reconnectAttempt,
    sendMessage: sendRealtimeMessage,
  } = useRealtimeChat(activeRoomId)
  const generalMessages = roomPreviewQueries[0]?.data
  const deckGuideMessages = roomPreviewQueries[1]?.data
  const partyRecruitmentMessages = roomPreviewQueries[2]?.data
  const questionAnswerMessages = roomPreviewQueries[3]?.data

  const activeRoom = useMemo(
    () => rooms.find((room) => room.id === activeRoomId),
    [activeRoomId, rooms],
  )
  const displayedRooms = useMemo(() => {
    const roomSnapshots = [
      { messages: generalMessages, roomId: COMMUNITY_CHAT_ROOM_IDS[0] },
      { messages: deckGuideMessages, roomId: COMMUNITY_CHAT_ROOM_IDS[1] },
      { messages: partyRecruitmentMessages, roomId: COMMUNITY_CHAT_ROOM_IDS[2] },
      { messages: questionAnswerMessages, roomId: COMMUNITY_CHAT_ROOM_IDS[3] },
    ]

    return roomSnapshots.reduce(
      (nextRooms, { messages, roomId }) =>
        roomId !== activeRoomId && messages
          ? applyRoomMessageSnapshot(nextRooms, roomId, messages)
          : nextRooms,
      rooms,
    )
  }, [
    activeRoomId,
    deckGuideMessages,
    generalMessages,
    partyRecruitmentMessages,
    questionAnswerMessages,
    rooms,
  ])
  const activeRoomName = activeRoom?.name ?? '채팅'
  const canSendMessages = isAuthenticated
  const connectionLabel = connectionStatus === 'connected'
    ? '실시간 연결됨'
    : isReconnecting
      ? '재연결 중'
      : connectionStatus === 'connecting'
      ? '연결 중'
      : '실시간 연결 대기'
  const reconnectNotice = isReconnecting
    ? `실시간 연결을 다시 시도하고 있습니다. (${reconnectAttempt}/${maxReconnectAttempts})`
    : ''
  const chatReadNotice = chatStatusMessage
    || reconnectNotice
    || chatErrorMessage
    || (hasReconnectFailed ? '실시간 연결을 복구하지 못했습니다.' : '')
    || (queryError ? '채팅 메시지를 불러오지 못했습니다.' : '')
  const chatNotice = chatReadNotice || (!canSendMessages ? '채팅은 조회할 수 있고 메시지 전송은 로그인 후 가능합니다.' : '')
  const isMessageDisabled = !canSendMessages || isSending || !activeRoom

  useEffect(() => {
    setRooms((currentRooms) => applyRoomMessageSnapshot(currentRooms, activeRoomId, activeMessages))
  }, [activeMessages, activeRoomId])

  const updateLastMessage = (roomId: string, message: string) => {
    setRooms((currentRooms) => updateChatRoomPreview(currentRooms, roomId, message))
  }

  const openPartyRecruitmentRoom = () => {
    onActiveRoomChange(PARTY_RECRUITMENT_ROOM_ID)
  }

  const preparePartyRoom = () => {
    openPartyRecruitmentRoom()
    setChatStatusMessage('')
  }

  const appendPartyMessage = () => {
    setChatStatusMessage('')
  }

  const sendMessage = async () => {
    const trimmedMessage = chatInput.trim()

    if (trimmedMessage.length === 0 || !activeRoom || !canSendMessages) {
      return
    }

    if (trimmedMessage.length > MAX_CHAT_MESSAGE_LENGTH) {
      setChatStatusMessage('메시지는 500자 이하로 입력해주세요.')
      return
    }

    try {
      const sentMessage = await sendRealtimeMessage({
        content: trimmedMessage,
        roomId: activeRoomId,
      })

      updateLastMessage(sentMessage.roomId, sentMessage.content)
      setChatInput('')
      setChatStatusMessage('')
    } catch {
      setChatStatusMessage('메시지 전송에 실패했습니다.')
    }
  }

  return {
    activeMessages,
    activeRoomId,
    activeRoomName,
    appendPartyMessage,
    chatInput,
    chatNotice,
    connectionLabel,
    currentUserId,
    isAuthenticated,
    isLoading,
    isMessageDisabled,
    isSendBlockedByAuth: !canSendMessages,
    preparePartyRoom,
    rooms: displayedRooms,
    sendMessage,
    setActiveRoomId: onActiveRoomChange,
    setChatInput,
  }
}
