import { useState } from 'react'
import { ChevronRight, Crown, Leaf, Sparkles, Swords, Users } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { PartyPost } from '../../../api/partyApi'
import styles from '../Dashboard.module.css'
import { partyFilters, type PartyFilter } from '../../Party/partyFilters'
import { getPartyPreviewStatusLabel } from '../utils/partyPreview'
import { usePartyPreviewPosts } from '../hooks/usePartyPreviewPosts'

const partyPostIcons: Record<PartyPost['icon'], typeof Crown> = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

const partyToneClassNames: Record<PartyPost['tone'], string> = {
  purple: styles.purple,
  green: styles.green,
  cyan: styles.cyan,
  gold: styles.gold,
}

const partyStatusClassNames: Record<PartyPost['status'], string> = {
  모집중: styles.partyStateOpen,
  대기중: styles.partyStateClosed,
}

function PartyFinderCard() {
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const navigate = useNavigate()
  const {
    emptyMessage,
    isLoading,
    isUnavailable,
    posts: visiblePosts,
    statusMessage,
  } = usePartyPreviewPosts(selectedFilter)

  return (
    <section className={`${styles.panel} ${styles.partyPanel}`}>
      <div className={styles.sideHeading}>
        <h2>파티원 찾기</h2>
        <button type="button" onClick={() => navigate('/party')}>
          더 보기
          <ChevronRight size={16} />
        </button>
      </div>
      <div aria-label="파티원 찾기 필터" className={styles.smallTabs} role="group">
        {partyFilters.map((filter) => (
          <button
            aria-pressed={selectedFilter === filter}
            className={selectedFilter === filter ? styles.selectedFilter : undefined}
            key={filter}
            onClick={() => setSelectedFilter(filter)}
            type="button"
          >
            {filter}
          </button>
        ))}
      </div>
      {statusMessage && (
        <p
          aria-live="polite"
          className={`${styles.partyStatus} ${isUnavailable ? styles.partyStatusFallback : ''}`}
          role="status"
        >
          {statusMessage}
        </p>
      )}
      <div aria-busy={isLoading} className={styles.partyList}>
        {isLoading ? (
          <p className={styles.emptyState}>잠시만 기다려주세요.</p>
        ) : visiblePosts.length > 0 ? (
          visiblePosts.map((post) => {
            const Icon = partyPostIcons[post.icon]

            return (
              <article className={styles.partyRow} key={post.id}>
                <span className={`${styles.partyIcon} ${partyToneClassNames[post.tone]}`}>
                  <Icon size={21} strokeWidth={2.2} />
                </span>
                <div>
                  <h3>{post.title}</h3>
                  <p>
                    <span>{post.mode}</span>
                    <span>{post.tier}</span>
                    <Users size={14} />
                    {post.capacity}
                    <span className={partyStatusClassNames[post.status]}>
                      {getPartyPreviewStatusLabel(post.status)}
                    </span>
                  </p>
                </div>
                <em>{post.close}</em>
              </article>
            )
          })
        ) : (
          <p className={styles.emptyState}>{emptyMessage}</p>
        )}
      </div>
    </section>
  )
}

export default PartyFinderCard
