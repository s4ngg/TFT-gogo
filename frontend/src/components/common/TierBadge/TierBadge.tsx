import styles from './TierBadge.module.css'

export type TierBadgeValue = 'S' | 'A+' | 'A' | 'B' | 'C' | 'D'

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
