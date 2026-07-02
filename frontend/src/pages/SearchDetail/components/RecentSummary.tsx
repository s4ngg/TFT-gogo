import type { CSSProperties } from 'react'
import type { MatchSummaryResponse } from '../../../api/searchApi'
import styles from '../SearchDetail.module.css'

export default function RecentSummary({ matches }: { matches: MatchSummaryResponse[] }) {
  if (matches.length === 0) return null

  const top4 = matches.filter((m) => m.placement <= 4).length
  const avgPlace = (matches.reduce((s, m) => s + m.placement, 0) / matches.length).toFixed(1)
  const top4Rate = ((top4 / matches.length) * 100).toFixed(1)
  const losses = matches.length - top4

  return (
    <section className={styles.summarySection}>
      <div
        className={styles.winRateDonut}
        style={{ '--pct': `${top4Rate}%` } as CSSProperties}
      >
        <div className={styles.winRateInner}>
          <strong className={styles.winRatePct}>{top4Rate}%</strong>
        </div>
      </div>
      <div className={styles.summaryStats}>
        <p className={styles.summaryStatLabel}>순방 확률</p>
        <p className={styles.summaryStatValue}>
          {top4}W {losses}L <span className={styles.summaryStatSub}>({top4Rate}%)</span>
        </p>
        <p className={styles.summaryStatLabel}>평균 순위</p>
        <p className={styles.summaryStatValue}>
          {avgPlace}<span className={styles.summaryStatTh}>th</span> / 8
        </p>
      </div>
    </section>
  )
}
