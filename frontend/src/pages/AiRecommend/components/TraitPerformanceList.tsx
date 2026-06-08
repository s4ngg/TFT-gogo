import TraitHexBadge from '../../../components/common/TraitHexBadge'
import type { AiRecommendTrait } from '../../../api/aiRecommendApi'
import styles from '../AiRecommend.module.css'

const MIN_TRAIT_GAMES = 5

interface TraitPerformanceListProps {
  traits: AiRecommendTrait[]
  bad?: boolean
}

function TraitPerformanceList({ traits, bad }: TraitPerformanceListProps) {
  const filtered = traits.filter((t) => t.games >= MIN_TRAIT_GAMES)

  if (filtered.length === 0) {
    return <p className={styles.traitEmpty}>5판 이상 플레이한 시너지가 없어요</p>
  }

  return (
    <div className={styles.myDeckList}>
      {filtered.map((t, i) => (
        <div key={t.name} className={styles.traitRow}>
          <span className={styles.myDeckNum}>{i + 1}</span>
          <TraitHexBadge count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
          <span className={styles.myDeckName}>{t.name}</span>
          <div className={styles.myDeckStats}>
            <span>{t.games}게임</span>
            <span className={styles.myAvg}>평균 {t.avgPlace}등</span>
            <span className={bad ? styles.myBadTop4 : styles.myTop4}>TOP4 {t.top4Rate}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

export default TraitPerformanceList
