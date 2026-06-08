import { Zap } from 'lucide-react'
import type { HeroAugmentSummary } from '../../Dashboard/dashboardData'
import styles from '../DeckDetail.module.css'

interface HeroAugmentsPanelProps {
  augments: HeroAugmentSummary[]
}

function HeroAugmentsPanel({ augments }: HeroAugmentsPanelProps) {
  if (augments.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Zap size={16} />
        <h2>영웅 증강</h2>
        <span className={styles.panelSub}>해금 증강 효과</span>
      </div>
      <div className={styles.heroAugList}>
        {augments.map((aug) => (
          <div key={`${aug.championId}-${aug.augmentName}`} className={styles.heroAugEntry}>
            <div className={styles.heroAugChamp}>
              {aug.imageUrl && (
                <img
                  src={aug.imageUrl}
                  alt={aug.championName}
                  className={styles.heroAugChampImg}
                  onError={(e) => { e.currentTarget.style.opacity = '0.3' }}
                />
              )}
              <span className={styles.heroAugChampName}>{aug.championName}</span>
            </div>
            <div className={styles.heroAugBadge}>{aug.augmentName}</div>
          </div>
        ))}
      </div>
    </section>
  )
}

export default HeroAugmentsPanel
