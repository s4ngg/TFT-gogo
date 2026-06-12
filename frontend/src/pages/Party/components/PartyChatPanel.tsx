import { type FormEvent } from 'react'
import { MessageCircle, Send, Users } from 'lucide-react'
import type { ChatMessage, ChatRoom } from '../types'
import styles from '../Party.module.css'

interface PartyChatPanelProps {
  activeMessages: ChatMessage[]
  activeRoomName: string
  chatInput: string
  onActiveRoomChange: (roomName: string) => void
  onChatInputChange: (value: string) => void
  onMessageSubmit: () => void
  rooms: ChatRoom[]
}

function PartyChatPanel({
  activeMessages,
  activeRoomName,
  chatInput,
  onActiveRoomChange,
  onChatInputChange,
  onMessageSubmit,
  rooms,
}: PartyChatPanelProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onMessageSubmit()
  }

  return (
    <section className={`${styles.panel} ${styles.chatPanel}`}>
      <div className={styles.panelHeader}>
        <div>
          <h2>실시간 채팅</h2>
          <p>현재 접속 중인 유저들과 빠르게 정보를 나눠보세요.</p>
        </div>
        <span className={styles.onlineBadge}>온라인 4,113</span>
      </div>

      <div className={styles.chatLayout}>
        <aside className={styles.channelList} aria-label="채팅 채널">
          {rooms.map((room) => (
            <button
              aria-pressed={activeRoomName === room.name}
              className={activeRoomName === room.name ? styles.activeChannel : undefined}
              onClick={() => onActiveRoomChange(room.name)}
              type="button"
              key={room.name}
            >
              <strong># {room.name}</strong>
              <span>
                <Users size={14} />
                {room.users}
              </span>
              <small>{room.lastMessage}</small>
            </button>
          ))}
        </aside>

        <div className={styles.chatWindow}>
          <div className={styles.chatWindowHeader}>
            <strong># {activeRoomName}</strong>
            <span>
              {activeMessages.length > 0
                ? `새 메시지 ${activeMessages.length}개`
                : '대화를 시작해보세요'}
            </span>
          </div>
          <div className={styles.messageList}>
            {activeMessages.length > 0 ? (
              activeMessages.map((chat) => (
                <article
                  className={chat.isMine ? styles.myMessage : undefined}
                  key={`${chat.roomName}-${chat.name}-${chat.time}-${chat.message}`}
                >
                  <div>
                    <strong>{chat.name}</strong>
                    <span>{chat.tier}</span>
                    <time>{chat.time}</time>
                  </div>
                  <p>{chat.message}</p>
                </article>
              ))
            ) : (
              <p className={styles.chatEmpty}>아직 이 채널에는 메시지가 없습니다.</p>
            )}
          </div>
          <form className={styles.messageForm} onSubmit={handleSubmit}>
            <MessageCircle size={19} />
            <input
              aria-label="채팅 메시지 입력"
              onChange={(event) => onChatInputChange(event.target.value)}
              placeholder="메시지를 입력하세요"
              value={chatInput}
            />
            <button type="submit" aria-label="메시지 보내기">
              <Send size={18} />
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}

export default PartyChatPanel
