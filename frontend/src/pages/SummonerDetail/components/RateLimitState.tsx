import { useEffect, useState } from 'react'
import styles from '../SummonerDetail.module.css'

export default function RateLimitState({ retryAfterSeconds }: { retryAfterSeconds: number }) {
  const [remaining, setRemaining] = useState(retryAfterSeconds)

  useEffect(() => {
    setRemaining(retryAfterSeconds)
    if (retryAfterSeconds <= 0) return
    const id = setInterval(() => setRemaining((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(id)
  }, [retryAfterSeconds])

  return (
    <div className={styles.emptyState}>
      <p className={styles.emptyTitle}>요청이 너무 많습니다</p>
      <p className={styles.emptyDesc}>
        {remaining > 0
          ? `${remaining}초 후 다시 시도해주세요`
          : '다시 검색할 수 있습니다'}
      </p>
    </div>
  )
}
