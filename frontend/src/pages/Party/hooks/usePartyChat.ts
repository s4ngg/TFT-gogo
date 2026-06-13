import { useMemo, useState } from 'react'
import { chatMessages, initialChatRooms } from '../data/partyMockData'
import type { ChatMessage } from '../types'
import { getCurrentTime } from '../utils/partyUtils'

const PARTY_ROOM_NAME = '파티 모집'

export function usePartyChat() {
  const [rooms, setRooms] = useState(initialChatRooms)
  const [activeRoomName, setActiveRoomName] = useState(initialChatRooms[0]?.name ?? '일반')
  const [messages, setMessages] = useState<ChatMessage[]>(chatMessages)
  const [chatInput, setChatInput] = useState('')

  const activeMessages = useMemo(
    () => messages.filter((message) => message.roomName === activeRoomName),
    [activeRoomName, messages],
  )

  const appendPartyMessage = (message: string) => {
    setActiveRoomName(PARTY_ROOM_NAME)
    setMessages((currentMessages) => [
      ...currentMessages,
      {
        roomName: PARTY_ROOM_NAME,
        name: '나',
        tier: 'Diamond',
        message,
        time: getCurrentTime(),
        isMine: true,
      },
    ])
    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.name === PARTY_ROOM_NAME ? { ...room, lastMessage: message } : room,
      ),
    )
  }

  const sendMessage = () => {
    const trimmedMessage = chatInput.trim()

    if (trimmedMessage.length === 0) {
      return
    }

    setMessages((currentMessages) => [
      ...currentMessages,
      {
        roomName: activeRoomName,
        name: '나',
        tier: 'Diamond',
        message: trimmedMessage,
        time: getCurrentTime(),
        isMine: true,
      },
    ])
    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.name === activeRoomName ? { ...room, lastMessage: trimmedMessage } : room,
      ),
    )
    setChatInput('')
  }

  return {
    activeMessages,
    activeRoomName,
    appendPartyMessage,
    chatInput,
    rooms,
    sendMessage,
    setActiveRoomName,
    setChatInput,
  }
}
