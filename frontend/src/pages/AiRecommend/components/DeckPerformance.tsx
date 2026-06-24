import { BarChart2, ThumbsDown, ThumbsUp } from 'lucide-react'
import type { AiRecommendTrait } from '../../../api/aiRecommendApi'
import TraitPerformanceList from './TraitPerformanceList'
import styles from '../AiRecommend.module.css'

interface DeckPerformanceProps {
  goodTraits: AiRecommendTrait[]
  badTraits: AiRecommendTrait[]
}

function DeckPerformance({ goodTraits, badTraits }: DeckPerformanceProps) {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <BarChart2 size={17} />
        <h2>내 시너지별 승률 성적</h2>
        <span className={styles.panelSub}>최근 20게임 기준</span>
      </div>
      <div className={styles.deckPerfSplit}>
        <div className={styles.deckPerfCol}>
          <div className={`${styles.deckPerfColHead} ${styles.deckPerfGood}`}>
            <ThumbsUp size={14} /> 잘 맞는 시너지
          </div>
          <TraitPerformanceList traits={goodTraits} />
        </div>
        <div className={styles.deckPerfDivider} />
        <div className={styles.deckPerfCol}>
          <div className={`${styles.deckPerfColHead} ${styles.deckPerfBad}`}>
            <ThumbsDown size={14} /> 잘 안 맞는 시너지
          </div>
          <TraitPerformanceList traits={badTraits} bad />
        </div>
      </div>
    </section>
  )
}

export default DeckPerformance
