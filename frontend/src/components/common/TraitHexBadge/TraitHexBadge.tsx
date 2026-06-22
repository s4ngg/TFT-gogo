import { useState, useEffect } from 'react'
import styles from './TraitHexBadge.module.css'
import type { TraitHexBadgeTone } from '../../../types/badges'

export interface TraitHexBadgeProps {
  count: number
  iconUrl: string
  name: string
  showCount?: boolean
  tone?: TraitHexBadgeTone
}

function TraitHexBadge({ count, iconUrl, name, showCount = true, tone = 'gold' }: TraitHexBadgeProps) {
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    setFailed(false)
  }, [iconUrl, name])

  return (
    <span className={`${styles.badge} ${styles[tone]}`} title={name}>
      <i className={styles.icon}>
        {failed
          ? <span className={styles.iconFallback}>{name.charAt(0).toUpperCase()}</span>
          : <img src={iconUrl} alt="" onError={() => setFailed(true)} />
        }
      </i>
      {showCount && <b className={styles.count}>{count}</b>}
    </span>
  )
}

export default TraitHexBadge
