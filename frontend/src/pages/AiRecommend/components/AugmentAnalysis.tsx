import { TrendingUp } from 'lucide-react'
import type { AiRecommendAugment } from '../../../api/aiRecommendApi'
import styles from '../AiRecommend.module.css'

function getAugmentTone(
  augment: AiRecommendAugment,
  best: AiRecommendAugment,
  worst: AiRecommendAugment,
): string {
  if (augment.name === best.name) return 'best'
  if (augment.name === worst.name) return 'worst'
  return 'default'
}

interface AugmentAnalysisProps {
  augments: AiRecommendAugment[]
}

function AugmentAnalysis({ augments }: AugmentAnalysisProps) {
  const best = augments[0]
  const worst = augments[augments.length - 1]

  if (!best || !worst) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <TrendingUp size={17} />
        <h2>증강 성적 분석</h2>
        <span className={styles.panelSub}>평균 등수 낮을수록 좋음</span>
      </div>
      <div className={styles.augmentList}>
        {augments.map((aug, i) => {
          const avgNum = parseFloat(aug.avgPlace)
          const tone = getAugmentTone(aug, best, worst)
          return (
            <div key={aug.name} className={styles.augRow}>
              <span className={styles.augRank}>{i + 1}</span>
              <span className={styles.augIcon}>{aug.icon}</span>
              <span className={styles.augName}>{aug.name}</span>
              <progress className={styles.augProgress} data-tone={tone} value={avgNum} max={8} />
              <span className={styles.augPlace} data-tone={tone}>{aug.avgPlace}등</span>
              <span className={styles.augGames}>{aug.games}게임</span>
            </div>
          )
        })}
      </div>
      <p className={styles.augTip}>
        💡 <b>{best.name}</b> 증강을 먹었을 때 평균 {best.avgPlace}등으로 가장 좋은 성적을 냈어요!
      </p>
    </section>
  )
}

export default AugmentAnalysis
