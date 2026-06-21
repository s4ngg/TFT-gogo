import styles from './RateLimitGauge.module.css'

interface RateLimitGaugeProps {
  used: number
  max: number
  label: string
  sub: string
}

function RateLimitGauge({ used, max, label, sub }: RateLimitGaugeProps) {
  const pct = max > 0 ? Math.min(100, Math.round((used / max) * 100)) : 0
  const isDanger = pct >= 80

  return (
    <div className={styles.gauge}>
      <div className={styles.header}>
        <span className={styles.label}>{label}</span>
        <span className={`${styles.count} ${isDanger ? styles.countDanger : ''}`}>
          {used} / {max} 사용
        </span>
      </div>
      <div className={styles.track}>
        <div
          className={`${styles.bar} ${isDanger ? styles.barDanger : ''}`}
          style={{ '--gauge-pct': `${pct}%` } as React.CSSProperties}
        />
      </div>
      <div className={styles.sub}>{sub}</div>
    </div>
  )
}

export default RateLimitGauge
