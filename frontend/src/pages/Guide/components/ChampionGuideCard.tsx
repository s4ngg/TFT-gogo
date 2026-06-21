import { Star } from 'lucide-react'
import type { ChampionGuide } from '../../../api/guide'
import { GuideChampionImage } from './GuideShared'
import styles from '../Guide.module.css'

interface ChampionGuideCardProps {
  championGuide: ChampionGuide
  isFavorite: boolean
  onFavoriteToggle: (championName: string) => void
  onOpen: (championGuide: ChampionGuide) => void
}

function ChampionGuideCard({
  championGuide,
  isFavorite,
  onFavoriteToggle,
  onOpen,
}: ChampionGuideCardProps) {
  return (
    <article
      className={styles.championCard}
      key={championGuide.name}
    >
      <button
        aria-pressed={isFavorite}
        className={`${styles.favoriteButton} ${isFavorite ? styles.favoriteActive : ''}`}
        onClick={(event) => {
          event.stopPropagation()
          onFavoriteToggle(championGuide.name)
        }}
        onKeyDown={(event) => {
          event.stopPropagation()
        }}
        title={isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
        type="button"
      >
        <Star size={14} />
      </button>
      <button
        className={styles.championOpenButton}
        onClick={() => {
          onOpen(championGuide)
        }}
        type="button"
      >
        <div className={styles.championPortrait}>
          <GuideChampionImage imageUrl={championGuide.imageUrl} name={championGuide.name} />
          <span className={styles.championCostBadge}>{championGuide.cost}</span>
        </div>
        <div className={styles.championInfo}>
          <strong>{championGuide.name}</strong>
          <span>{championGuide.role}</span>
        </div>
      </button>
      <div className={styles.championTooltip} role="tooltip">
        <div className={styles.tooltipTop}>
          <GuideChampionImage decorative imageUrl={championGuide.imageUrl} name={championGuide.name} />
          <div>
            <strong>{championGuide.name}</strong>
            <span>{championGuide.traits.join(' / ')}</span>
          </div>
        </div>
        <dl className={styles.statGrid}>
          <div><dt>체력</dt><dd>{championGuide.stats.hp}</dd></div>
          <div><dt>공격력</dt><dd>{championGuide.stats.ad}</dd></div>
          <div><dt>공속</dt><dd>{championGuide.stats.attackSpeed}</dd></div>
          <div><dt>마나</dt><dd>{championGuide.stats.mana}</dd></div>
          <div><dt>방어</dt><dd>{championGuide.stats.armor}</dd></div>
          <div><dt>마저</dt><dd>{championGuide.stats.mr}</dd></div>
        </dl>
        <p>권장 배치: {championGuide.position}</p>
      </div>
    </article>
  )
}

export default ChampionGuideCard
