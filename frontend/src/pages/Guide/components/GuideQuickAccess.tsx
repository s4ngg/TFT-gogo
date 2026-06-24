import type { GuideTab, RecentGuide } from '../../../api/guide'
import styles from '../Guide.module.css'

interface GuideQuickAccessProps {
  favoriteChampions: string[]
  onJump: (tab: GuideTab, query: string, label?: string) => void
  recentGuides: RecentGuide[]
}

function GuideQuickAccess({
  favoriteChampions,
  onJump,
  recentGuides,
}: GuideQuickAccessProps) {
  if (favoriteChampions.length === 0 && recentGuides.length === 0) return null

  return (
    <section className={styles.quickAccess} aria-label="빠른 이동">
      {favoriteChampions.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>즐겨찾기</strong>
          {favoriteChampions.slice(0, 6).map((name) => (
            <button key={name} onClick={() => onJump('champions', name, name)} type="button">
              {name}
            </button>
          ))}
        </div>
      )}
      {recentGuides.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>최근 본 가이드</strong>
          {recentGuides.slice(0, 6).map((guide) => (
            <button
              key={`${guide.tab}-${guide.query}`}
              onClick={() => onJump(guide.tab, guide.query, guide.label)}
              type="button"
            >
              {guide.label}
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

export default GuideQuickAccess
