import styles from './TraitHexBadge.module.css'

export type TraitHexBadgeTone = 'gold' | 'silver'

export interface TraitHexBadgeProps {
  count: number
  iconUrl: string
  name: string
  tone?: TraitHexBadgeTone
}

function TraitHexBadge({ count, iconUrl, name, tone = 'gold' }: TraitHexBadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[tone]}`} title={name}>
      <i className={styles.icon}>
        <img src={iconUrl} alt="" />
      </i>
      <b className={styles.count}>{count}</b>
    </span>
  )
}

export default TraitHexBadge
