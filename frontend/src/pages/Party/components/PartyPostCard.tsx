import { Clock3, Crown, Leaf, Sparkles, Swords, Users } from 'lucide-react'
import type { PartyPost } from '../types'
import { parseCapacity } from '../utils/partyUtils'
import styles from '../Party.module.css'

const partyIconMap = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

interface PartyPostCardProps {
  hasJoinedOtherPost: boolean
  isJoined: boolean
  isJoinPending: boolean
  onJoinToggle: (postId: string) => void
  post: PartyPost
}

function PartyPostCard({
  hasJoinedOtherPost,
  isJoined,
  isJoinPending,
  onJoinToggle,
  post,
}: PartyPostCardProps) {
  const Icon = partyIconMap[post.icon]
  const { current, total } = parseCapacity(post.capacity)
  const isFull = current >= total

  return (
    <article className={styles.partyCard}>
      <div className={`${styles.partyIcon} ${styles[post.tone]}`}>
        <Icon size={28} strokeWidth={2.2} />
      </div>
      <div className={styles.partyContent}>
        <div className={styles.partyTitleLine}>
          <h3>{post.title}</h3>
          <span>{isFull ? '마감' : post.status}</span>
        </div>
        <p>{post.description}</p>
        <div className={styles.partyMeta}>
          <span>{post.mode}</span>
          <span>{post.tier}</span>
          <span>
            <Users size={15} />
            {post.capacity}
          </span>
          <span>
            <Clock3 size={15} />
            {post.close}
          </span>
        </div>
        <div className={styles.partyTags}>
          {post.tags.map((tag, index) => (
            <small key={`${post.id}-${tag}-${index}`}>{tag}</small>
          ))}
        </div>
      </div>
      <button
        type="button"
        aria-pressed={isJoined}
        className={styles.joinButton}
        disabled={isJoinPending || (isFull && !isJoined) || hasJoinedOtherPost}
        onClick={() => onJoinToggle(post.id)}
      >
        {isJoinPending ? '처리중' : isJoined ? '참여중' : isFull ? '마감' : hasJoinedOtherPost ? '잠김' : '참여'}
      </button>
    </article>
  )
}

export default PartyPostCard
