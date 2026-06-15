import { useEffect, useMemo, useState } from 'react'
import useAuthStore from '../../../store/useAuthStore'
import { initialChatRooms } from '../data/partyMockData'
import type { PartyPost } from '../types'
import { createPartyChatRoom, upsertChatRoom } from '../utils/partyChatRooms'
import { useRealtimeChat } from './useRealtimeChat'

export function usePartyChat() {
  const authUser = useAuthStore((state) => state.user)
  const authToken = useAuthStore((state) => state.token)
  const [rooms, setRooms] = useState(initialChatRooms)
  const [activeRoomId, setActiveRoomId] = useState(initialChatRooms[0]?.id ?? 'general')
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
  } = useRealtimeChat(activeRoomId, Boolean(authToken))

  const currentUserName = authUser?.nickname ?? authUser?.summonerName ?? '나'
  const activeRoom = useMemo(
    () => rooms.find((room) => room.id === activeRoomId),
    [activeRoomId, rooms],
  )
  const activeRoomName = activeRoom?.name ?? '채팅'
  const isChatAvailable = Boolean(authToken)
  const connectionLabel = connectionStatus === 'connected'
    ? '실시간 연결됨'
    : connectionStatus === 'connecting'
      ? '연결 중'
      : '실시간 연결 대기'
  const chatNotice = !isChatAvailable
    ? '로그인 후 채팅을 조회하고 메시지를 보낼 수 있습니다.'
    : chatStatusMessage || chatErrorMessage || (queryError ? '채팅 메시지를 불러오지 못했습니다.' : '')
  const isMessageDisabled = !isChatAvailable || isSending || !activeRoom

  useEffect(() => {
    const lastMessage = activeMessages[activeMessages.length - 1]

    if (!lastMessage) {
      return
    }

    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.id === lastMessage.roomId ? { ...room, lastMessage: lastMessage.content } : room,
      ),
    )
  }, [activeMessages])

  const updateLastMessage = (roomId: string, message: string) => {
    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.id === roomId ? { ...room, lastMessage: message } : room,
      ),
    )
  }

  const preparePartyRoom = (post: PartyPost, lastMessage?: string) => {
    setRooms((currentRooms) => upsertChatRoom(currentRooms, createPartyChatRoom(post, lastMessage)))
    setActiveRoomId(post.chatRoomId)
  }

  const appendPartyMessage = (post: PartyPost, message: string) => {
    preparePartyRoom(post, message)
    setChatStatusMessage('')

    if (!isChatAvailable) {
      setChatStatusMessage('로그인 후 파티 채팅 알림을 보낼 수 있습니다.')
      return
    }

    void sendRealtimeMessage({
      content: message,
      roomId: post.chatRoomId,
    })
      .then((sentMessage) => updateLastMessage(sentMessage.roomId, sentMessage.content))
      .catch(() => setChatStatusMessage('참여 상태는 반영됐지만 채팅 알림 전송에 실패했습니다.'))
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
    isLoading,
    isMessageDisabled,
    preparePartyRoom,
    rooms,
    sendMessage,
    setActiveRoomId,
    setChatInput,
  }
}
