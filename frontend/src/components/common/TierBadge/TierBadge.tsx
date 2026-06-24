import styles from './TierBadge.module.css'
import type { TierBadgeValue } from '../../../types/badges'

interface TierBadgeProps {
  value: TierBadgeValue
}

const TIER_LABELS: Record<TierBadgeValue, string> = {
  A: 'A',
  B: 'B',
  C: 'C',
  D: 'D',
  S: 'S',
  UNKNOWN: '?',
}

function TierBadge({ value }: TierBadgeProps) {
  return (
    <strong className={styles.badge} data-tier={value} title={value === 'UNKNOWN' ? '티어 미분류' : `${value} 티어`}>
      <span>{TIER_LABELS[value]}</span>
    </strong>
  )
}

export default TierBadge
