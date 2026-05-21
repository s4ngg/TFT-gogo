import { BadgePlus, Crosshair, Gem, LayoutGrid, Sparkles } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import styles from '../Dashboard.module.css'

const aiOptions = [
  { label: '증강', icon: <BadgePlus size={15} strokeWidth={2.1} /> },
  { label: '아이템', icon: <Gem size={15} strokeWidth={2.1} /> },
  { label: '상성', icon: <Crosshair size={15} strokeWidth={2.1} /> },
  { label: '배치', icon: <LayoutGrid size={15} strokeWidth={2.1} /> },
]

function AiDeckRecommend() {
  const navigate = useNavigate()

  return (
    <section className={`${styles.panel} ${styles.aiPanel}`}>
      <div>
        <h2>
          <span>AI</span> 덱추천
        </h2>
        <p>AI가 당신의 덱을 분석하고 최적의 전략을 제안해 드려요.</p>
      </div>
      <div className={styles.aiOptions}>
        {aiOptions.map((option) => (
          <div className={styles.aiOption} key={option.label}>
            <span className={styles.aiOptionIcon} aria-hidden="true">
              {option.icon}
            </span>
            <span className={styles.aiOptionLabel}>{option.label}</span>
          </div>
        ))}
      </div>
      <button type="button" onClick={() => navigate('/ai-recommend')}>
        <Sparkles size={30} />
        분석 시작
      </button>
    </section>
  )
}

export default AiDeckRecommend
