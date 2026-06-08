import styles from '../AiRecommend.module.css'

interface StatCardProps {
  label: string
  value: string
  tone?: 'default' | 'purple' | 'green' | 'gold'
}

function StatCard({ label, value, tone = 'default' }: StatCardProps) {
  return (
    <div className={styles.statCard} data-tone={tone}>
      <small>{label}</small>
      <strong>{value}</strong>
    </div>
  )
}

export default StatCard
