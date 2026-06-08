import { Trophy } from 'lucide-react'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { getTraitName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { TraitSummary } from '../../Dashboard/dashboardData'
import styles from '../DeckDetail.module.css'

const BP_TIER_STYLES: Record<string, string> = {
  bronze:    styles.bpBronze,
  silver:    styles.bpSilver,
  gold:      styles.bpGold,
  prismatic: styles.bpPrismatic,
}

interface TraitPanelProps {
  displayTraits: TraitSummary[]
  level: number
  locale: TFTLocale | undefined
}

function TraitPanel({ displayTraits, level, locale }: TraitPanelProps) {
  if (displayTraits.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Trophy size={16} />
        <h2>시너지 구성</h2>
        <span className={styles.panelSub}>Lv.{level} 기준</span>
      </div>
      <div className={styles.traitList}>
        {displayTraits.map((trait) => {
          const traitDetail = locale?.traitDetailBySuffix.get(trait.name.toLowerCase())
          return (
            <div key={trait.name} className={styles.traitItem}>
              <TraitHexBadge
                count={trait.count}
                iconUrl={trait.iconUrl}
                name={getTraitName(trait.name, locale)}
                tone={trait.tone}
              />
              <div className={styles.traitInfo}>
                <span className={styles.traitName}>{getTraitName(trait.name, locale)}</span>
                {traitDetail && traitDetail.breakpoints.length > 0 && (
                  <div className={styles.breakpoints}>
                    {traitDetail.breakpoints.map((bp) => {
                      const tierClass = BP_TIER_STYLES[bp.tier] ?? ''
                      const activeClass = trait.count >= bp.minUnits ? styles.bpActive : ''
                      return (
                        <span
                          key={bp.minUnits}
                          className={`${styles.bpPip} ${tierClass} ${activeClass}`}
                        >
                          {bp.minUnits}
                        </span>
                      )
                    })}
                  </div>
                )}
              </div>
              <span className={styles.traitCount}>{trait.count}조각</span>
            </div>
          )
        })}
      </div>
    </section>
  )
}

export default TraitPanel
