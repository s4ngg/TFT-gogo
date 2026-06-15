import { ChevronRight, MessageCircle, Users } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import {
  COMMUNITY_CHAT_ROOM_QUERY_PARAM,
  type CommunityChatRoomId,
} from '../../../constants/communityChatRooms'
import { useCommunityChatPreviews } from '../../../hooks/useCommunityChatPreviews'
import styles from '../Dashboard.module.css'

function buildChatRoomSearch(roomId: CommunityChatRoomId) {
  const params = new URLSearchParams()
  params.set(COMMUNITY_CHAT_ROOM_QUERY_PARAM, roomId)

  return `?${params.toString()}`
}

function LiveChat() {
  const navigate = useNavigate()
  const chatChannels = useCommunityChatPreviews()

  return (
    <section className={`${styles.panel} ${styles.chatPanel}`}>
      <div className={styles.sideHeading}>
        <h2>실시간 채팅</h2>
        <button type="button" onClick={() => navigate('/party')}>
          더 보기
          <ChevronRight size={16} />
        </button>
      </div>
      <div className={styles.chatList}>
        {chatChannels.map((channel) => (
          <Link
            aria-label={`${channel.name} 채팅 열기`}
            className={styles.chatRow}
            key={channel.id}
            to={{
              pathname: '/party',
              search: buildChatRoomSearch(channel.id),
            }}
          >
            <strong>#</strong>
            <b>{channel.name}</b>
            <span>
              <Users size={13} />
              {channel.users}
            </span>
            <p>{channel.message}</p>
            <time>{channel.time}</time>
          </Link>
        ))}
      </div>
      <button type="button" className={styles.chatButton} onClick={() => navigate('/party')}>
        <MessageCircle size={20} />
        채팅 열기
      </button>
    </section>
  )
}

export default LiveChat
