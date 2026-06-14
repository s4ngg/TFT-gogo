import type { CSSProperties } from 'react'
import type { MatchSummaryResponse } from '../../../api/summonerApi'
import styles from '../SummonerDetail.module.css'

export default function RecentSummary({ matches }: { matches: MatchSummaryResponse[] }) {
  const recent = matches.slice(0, 30)

  if (recent.length === 0) return null

  const top4 = recent.filter((m) => m.placement <= 4).length
  const avgPlace = (recent.reduce((s, m) => s + m.placement, 0) / recent.length).toFixed(1)
  const top4Rate = ((top4 / recent.length) * 100).toFixed(1)
  const losses = recent.length - top4

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
