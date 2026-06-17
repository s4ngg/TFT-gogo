import { AlertTriangle, ArrowUpRight } from 'lucide-react'
import styles from '../PatchNotes.module.css'

interface PatchSummaryGridProps {
  buffCount: number
  nerfCount: number
}

function PatchSummaryGrid({
  buffCount,
  nerfCount,
}: PatchSummaryGridProps) {
  return (
    <section className={styles.summaryGrid} aria-label="패치 핵심 지표">
      <article className={styles.summaryCard}>
        <span className={styles.summaryIcon}>
          <ArrowUpRight size={18} />
        </span>
        <div>
          <strong>{buffCount}</strong>
          <p>상향 항목</p>
        </div>
      </article>
      <article className={styles.summaryCard}>
        <span className={`${styles.summaryIcon} ${styles.warnIcon}`}>
          <AlertTriangle size={18} />
        </span>
        <div>
          <strong>{nerfCount}</strong>
          <p>하향 항목</p>
        </div>
      </article>
    </section>
  )
}

export default PatchSummaryGrid
