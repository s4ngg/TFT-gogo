import { useMemo, useState } from 'react'
import { ChevronRight, Crown, Leaf, Sparkles, Swords, Users } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { partyPosts, type PartyPost } from '../dashboardData'
import styles from '../Dashboard.module.css'
import { partyFilters, type PartyFilter } from '../../Party/partyFilters'

const partyPostIcons: Record<PartyPost['icon'], typeof Crown> = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  goal: Swords,
}

function PartyFinderCard() {
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const navigate = useNavigate()
  const visiblePosts = useMemo(() => {
    const filteredPosts = selectedFilter === '전체'
      ? partyPosts
      : partyPosts.filter((post) => post.mode === selectedFilter)

    return filteredPosts.slice(0, 4)
  }, [selectedFilter])

  return (
    <section className={`${styles.panel} ${styles.partyPanel}`}>
      <div className={styles.sideHeading}>
        <h2>파티원 찾기</h2>
        <button type="button" onClick={() => navigate('/party')}>
          더 보기
          <ChevronRight size={16} />
        </button>
      </div>
      <div className={styles.smallTabs}>
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
      <div className={styles.partyList}>
        {visiblePosts.length > 0 ? (
          visiblePosts.map((post) => {
            const Icon = partyPostIcons[post.icon]

            return (
              <article className={styles.partyRow} key={post.title}>
                <span className={`${styles.partyIcon} ${styles[post.tone]}`}>
                  <Icon size={21} strokeWidth={2.2} />
                </span>
                <div>
                  <h3>{post.title}</h3>
                  <p>
                    <span>{post.mode}</span>
                    <span>{post.tier}</span>
                    <Users size={14} />
                    {post.count}
                  </p>
                </div>
                <em>{post.close}</em>
              </article>
            )
          })
        ) : (
          <p className={styles.emptyState}>조건에 맞는 모집글이 없습니다.</p>
        )}
      </div>
    </section>
  )
}

export default PartyFinderCard
