import { Trophy } from 'lucide-react'
import type { RewardRow } from '../../../api/guide'
import styles from '../Guide.module.css'

interface AugmentRewardPanelProps {
  rewardRows: RewardRow[]
}

function AugmentRewardPanel({ rewardRows }: AugmentRewardPanelProps) {
  return (
    <aside className={styles.rewardPanel}>
      <div className={styles.panelHeading}>
        <Trophy size={17} />
        <h2>보상표</h2>
      </div>
      <div className={styles.rewardList}>
        {rewardRows.map((row) => (
          <div className={styles.rewardRow} key={`${row.stage}-${row.condition}`}>
            <b>{row.stage}</b>
            <strong>{row.condition}</strong>
            <span>{row.reward}</span>
          </div>
        ))}
      </div>
    </aside>
  )
}

export default AugmentRewardPanel
