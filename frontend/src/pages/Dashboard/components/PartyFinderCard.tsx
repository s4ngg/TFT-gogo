import { ChevronRight, Crown, Leaf, Sparkles, Swords, Users } from 'lucide-react'
import { partyPosts, type PartyPost } from '../dashboardData'
import styles from '../Dashboard.module.css'

const partyPostIcons: Record<PartyPost['icon'], typeof Crown> = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  goal: Swords,
}

function PartyFinderCard() {
  return (
    <section className={`${styles.panel} ${styles.partyPanel}`}>
      <div className={styles.sideHeading}>
        <h2>파티원 찾기</h2>
        <button type="button">
          더 보기
          <ChevronRight size={16} />
        </button>
      </div>
      <div className={styles.smallTabs}>
        <button type="button" className={styles.selectedFilter}>전체</button>
        <button type="button">랭크</button>
        <button type="button">일반</button>
        <button type="button">커스텀</button>
      </div>
      <div className={styles.partyList}>
        {partyPosts.slice(0, 3).map((post) => {
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
        })}
      </div>
    </section>
  )
}

export default PartyFinderCard
