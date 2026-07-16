import { Clock3, Crown, Leaf, Sparkles, Swords, Users } from 'lucide-react'
import type { PartyPost } from '../types'
import { getPartyJoinActionState, parseCapacity } from '../utils/partyUtils'
import styles from '../Party.module.css'

const partyIconMap = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

interface PartyPostCardProps {
  hasJoinedOtherPost: boolean
  isAuthenticated: boolean
  isJoined: boolean
  isJoinPending: boolean
  isOwner: boolean
  onJoinToggle: (postId: string) => void
  post: PartyPost
}

function PartyPostCard({
  hasJoinedOtherPost,
  isAuthenticated,
  isJoined,
  isJoinPending,
  isOwner,
  onJoinToggle,
  post,
}: PartyPostCardProps) {
  const Icon = partyIconMap[post.icon]
  const { current, total } = parseCapacity(post.capacity)
  const isFull = current >= total
  const isClosed = post.isClosed || isFull
  const joinAction = getPartyJoinActionState({
    hasJoinedOtherPost,
    isAuthenticated,
    isClosed,
    isFull,
    isJoined,
    isJoinPending,
    isOwner,
  })

  return (
    <article className={styles.partyCard}>
      <div className={`${styles.partyIcon} ${styles[post.tone]}`}>
        <Icon size={28} strokeWidth={2.2} />
      </div>
      <div className={styles.partyContent}>
        <div className={styles.partyTitleLine}>
          <h3>{post.title}</h3>
          <span>{isClosed ? '마감' : post.status}</span>
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
        disabled={joinAction.disabled}
        onClick={() => onJoinToggle(post.id)}
      >
        {joinAction.label}
      </button>
    </article>
  )
}

export default PartyPostCard
