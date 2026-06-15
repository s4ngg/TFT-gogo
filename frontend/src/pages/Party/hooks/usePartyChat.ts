import { useEffect, useMemo, useState } from 'react'
import { PARTY_RECRUITMENT_ROOM_ID } from '../../../constants/communityChatRooms'
import type { CommunityChatRoomId } from '../../../constants/communityChatRooms'
import { initialChatRooms } from '../data/partyMockData'
import type { PartyPost } from '../types'
import { updateChatRoomPreview, updatePartyRecruitmentPreview } from '../utils/partyChatRooms'
import { usePartyAuth } from './usePartyAuth'
import { useRealtimeChat } from './useRealtimeChat'

interface UsePartyChatOptions {
  activeRoomId: CommunityChatRoomId
  onActiveRoomChange: (roomId: CommunityChatRoomId) => void
}

export function usePartyChat({ activeRoomId, onActiveRoomChange }: UsePartyChatOptions) {
  const { displayName: currentUserName, isAuthenticated } = usePartyAuth()
  const [rooms, setRooms] = useState(initialChatRooms)
  const [chatInput, setChatInput] = useState('')
  const [chatStatusMessage, setChatStatusMessage] = useState('')
  const {
    connectionStatus,
    errorMessage: chatErrorMessage,
    isLoading,
    isSending,
    messages: activeMessages,
    queryError,
    sendMessage: sendRealtimeMessage,
  } = useRealtimeChat(activeRoomId)

  const activeRoom = useMemo(
    () => rooms.find((room) => room.id === activeRoomId),
    [activeRoomId, rooms],
  )
  const activeRoomName = activeRoom?.name ?? '채팅'
  const isChatAvailable = isAuthenticated
  const connectionLabel = connectionStatus === 'connected'
    ? '실시간 연결됨'
    : connectionStatus === 'connecting'
      ? '연결 중'
      : '실시간 연결 대기'
  const chatReadNotice = chatStatusMessage || chatErrorMessage || (queryError ? '채팅 메시지를 불러오지 못했습니다.' : '')
  const chatNotice = chatReadNotice || (!isChatAvailable ? '채팅은 조회할 수 있고 메시지 전송은 로그인 후 가능합니다.' : '')
  const isMessageDisabled = !isChatAvailable || isSending || !activeRoom

  useEffect(() => {
    const lastMessage = activeMessages[activeMessages.length - 1]

    if (!lastMessage) {
      return
    }

    setRooms((currentRooms) => updateChatRoomPreview(currentRooms, lastMessage.roomId, lastMessage.content))
  }, [activeMessages])

  const updateLastMessage = (roomId: string, message: string) => {
    setRooms((currentRooms) => updateChatRoomPreview(currentRooms, roomId, message))
  }

  const openPartyRecruitmentRoom = (message: string) => {
    setRooms((currentRooms) => updatePartyRecruitmentPreview(currentRooms, message))
    onActiveRoomChange(PARTY_RECRUITMENT_ROOM_ID)
  }

  const sendPartyRecruitmentMessage = (message: string, failureMessage: string) => {
    setChatStatusMessage('')

    if (!isChatAvailable) {
      setChatStatusMessage('로그인 후 파티 채팅 알림을 보낼 수 있습니다.')
      return
    }

    void sendRealtimeMessage({
      content: message,
      roomId: PARTY_RECRUITMENT_ROOM_ID,
    })
      .then((sentMessage) => updateLastMessage(sentMessage.roomId, sentMessage.content))
      .catch(() => setChatStatusMessage(failureMessage))
  }

  const preparePartyRoom = (post: PartyPost, lastMessage?: string) => {
    const nextMessage = lastMessage ?? `${post.title} 모집글이 등록되었습니다.`

    openPartyRecruitmentRoom(nextMessage)
    sendPartyRecruitmentMessage(nextMessage, '모집글은 등록됐지만 채팅 알림 전송에 실패했습니다.')
  }

  const appendPartyMessage = (post: PartyPost, message: string) => {
    openPartyRecruitmentRoom(message)
    sendPartyRecruitmentMessage(message, '참여 상태는 반영됐지만 채팅 알림 전송에 실패했습니다.')
  }

  const sendMessage = async () => {
    const trimmedMessage = chatInput.trim()

    if (trimmedMessage.length === 0 || !activeRoom || !isChatAvailable) {
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
    currentUserName,
    isAuthenticated,
    isLoading,
    isMessageDisabled,
    isSendBlockedByAuth: !isChatAvailable,
    preparePartyRoom,
    rooms,
    sendMessage,
    setActiveRoomId: onActiveRoomChange,
    setChatInput,
  }
}
