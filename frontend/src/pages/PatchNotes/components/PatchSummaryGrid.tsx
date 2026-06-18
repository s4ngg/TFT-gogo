import { ListChecks, Sparkles } from 'lucide-react'
import styles from '../PatchNotes.module.css'

interface PatchSummaryGridProps {
  newCount: number
  totalCount: number
}

function PatchSummaryGrid({
  newCount,
  totalCount,
}: PatchSummaryGridProps) {
  return (
    <section className={styles.summaryGrid} aria-label="패치 핵심 지표">
      <article className={styles.summaryCard}>
        <span className={styles.summaryIcon}>
          <ListChecks size={18} />
        </span>
        <div>
          <strong>{totalCount}</strong>
          <p>전체 변경사항</p>
        </div>
      </article>
      <article className={styles.summaryCard}>
        <span className={`${styles.summaryIcon} ${styles.newSummaryIcon}`}>
          <Sparkles size={18} />
        </span>
        <div>
          <strong>{newCount}</strong>
          <p>신규 항목</p>
        </div>
      </article>
    </section>
  )
}

export default PatchSummaryGrid
