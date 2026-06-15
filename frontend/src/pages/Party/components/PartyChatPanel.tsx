import { type FormEvent } from 'react'
import { MessageCircle, Send, Users } from 'lucide-react'
import type { ChatMessage } from '../../../api/chatApi'
import type { ChatRoom } from '../types'
import styles from '../Party.module.css'

interface PartyChatPanelProps {
  activeMessages: ChatMessage[]
  activeRoomId: string
  activeRoomName: string
  chatInput: string
  chatNotice: string
  connectionLabel: string
  currentUserName: string
  isLoading: boolean
  isMessageDisabled: boolean
  onActiveRoomChange: (roomId: string) => void
  onChatInputChange: (value: string) => void
  onMessageSubmit: () => Promise<void>
  rooms: ChatRoom[]
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

function PartyChatPanel({
  activeMessages,
  activeRoomId,
  activeRoomName,
  chatInput,
  chatNotice,
  connectionLabel,
  currentUserName,
  isLoading,
  isMessageDisabled,
  onActiveRoomChange,
  onChatInputChange,
  onMessageSubmit,
  rooms,
}: PartyChatPanelProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void onMessageSubmit()
  }

  return (
    <section className={`${styles.panel} ${styles.chatPanel}`}>
      <div className={styles.panelHeader}>
        <div>
          <h2>실시간 채팅</h2>
          <p>현재 접속 중인 유저들과 빠르게 정보를 나눠보세요.</p>
        </div>
        <span className={styles.onlineBadge}>{connectionLabel}</span>
      </div>

      <div className={styles.chatLayout}>
        <aside className={styles.channelList} aria-label="채팅 채널">
          {rooms.map((room) => (
            <button
              aria-pressed={activeRoomId === room.id}
              className={activeRoomId === room.id ? styles.activeChannel : undefined}
              onClick={() => onActiveRoomChange(room.id)}
              type="button"
              key={room.id}
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
              {isLoading
                ? '메시지 불러오는 중'
                : activeMessages.length > 0
                  ? `새 메시지 ${activeMessages.length}개`
                  : '대화를 시작해보세요'}
            </span>
          </div>
          {chatNotice && (
            <p className={styles.chatStatus} role="status" aria-live="polite">
              {chatNotice}
            </p>
          )}
          <div className={styles.messageList} role="log" aria-live="polite">
            {activeMessages.length > 0 ? (
              activeMessages.map((chat) => (
                <article
                  className={chat.senderName === currentUserName ? styles.myMessage : undefined}
                  key={chat.id}
                >
                  <div>
                    <strong>{chat.senderName}</strong>
                    <span>{chat.tier ?? 'Unranked'}</span>
                    <time dateTime={chat.createdAt}>{formatMessageTime(chat.createdAt)}</time>
                  </div>
                  <p>{chat.content}</p>
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
              disabled={isMessageDisabled}
              onChange={(event) => onChatInputChange(event.target.value)}
              placeholder={isMessageDisabled ? '로그인 후 채팅 가능' : '메시지를 입력하세요'}
              value={chatInput}
            />
            <button
              type="submit"
              aria-disabled={isMessageDisabled}
              aria-label="메시지 보내기"
              disabled={isMessageDisabled}
            >
              <Send size={18} />
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}

export default PartyChatPanel
