import { BookOpen } from 'lucide-react'
import type { MetaDeck } from '../../Dashboard/dashboardData'
import styles from '../DeckDetail.module.css'

interface PlayGuide { early: string; mid: string; late: string }

interface PlayGuidePanelProps {
  deck: MetaDeck
}

function PlayGuidePanel({ deck }: PlayGuidePanelProps) {
  if (!deck.playGuide) return null

  let guide: PlayGuide
  try { guide = JSON.parse(deck.playGuide) as PlayGuide } catch { return null }
  if (!guide.early && !guide.mid && !guide.late) return null

  const phases: { key: keyof PlayGuide; label: string }[] = [
    { key: 'early', label: '초반' },
    { key: 'mid', label: '중반' },
    { key: 'late', label: '후반' },
  ]

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <BookOpen size={16} />
        <h2>운영 방법</h2>
      </div>
      <div className={styles.guideList}>
        {phases.filter((p) => guide[p.key]).map((p) => (
          <div key={p.key} className={styles.guidePhase}>
            <span className={styles.guidePhaseLabel}>{p.label}</span>
            <p className={styles.guidePhaseText}>{guide[p.key]}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

export default PlayGuidePanel
