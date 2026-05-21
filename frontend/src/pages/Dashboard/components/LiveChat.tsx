import { ChevronRight, MessageCircle, Users } from 'lucide-react'
import { chatChannels } from '../dashboardData'
import styles from '../Dashboard.module.css'

function LiveChat() {
  return (
    <section className={`${styles.panel} ${styles.chatPanel}`}>
      <div className={styles.sideHeading}>
        <h2>실시간 채팅</h2>
        <button type="button">
          채널 목록
          <ChevronRight size={16} />
        </button>
      </div>
      <div className={styles.chatList}>
        {chatChannels.map((channel) => (
          <article className={styles.chatRow} key={channel.name}>
            <strong>#</strong>
            <b>{channel.name}</b>
            <span>
              <Users size={13} />
              {channel.users}
            </span>
            <p>{channel.message}</p>
            <time>{channel.time}</time>
          </article>
        ))}
      </div>
      <button type="button" className={styles.chatButton}>
        <MessageCircle size={20} />
        채팅 열기
      </button>
    </section>
  )
}

export default LiveChat
