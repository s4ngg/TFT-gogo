import styles from './TierBadge.module.css'
import type { TierBadgeValue } from '../../../types/badges'

interface TierBadgeProps {
  value: TierBadgeValue
}

function TierBadge({ value }: TierBadgeProps) {
  return (
    <strong className={styles.badge} data-tier={value}>
      <span>{value}</span>
    </strong>
  )
}

export default TierBadge
