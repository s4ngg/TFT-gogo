import { Bot, ChevronRight, Link2, Sparkles, TrendingUp, Trophy } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import styles from '../AiRecommend.module.css'

function ConnectPrompt() {
  const navigate = useNavigate()

  return (
    <div className={styles.promptWrap}>
      <div className={styles.promptCard}>
        <div className={styles.promptIcon}>
          <Bot size={40} />
        </div>
        <h2>소환사 전적을 연동하면<br />개인 맞춤 AI 추천을 받을 수 있어요</h2>
        <p>
          최근 20게임을 분석해 내가 자주 쓰는 덱, 승률 높은 증강 조합,
          그리고 나에게 맞는 덱을 AI가 추천해 드립니다.
        </p>
        <div className={styles.promptFeatures}>
          <span><Trophy size={15} /> 내 자주 쓴 덱 분석</span>
          <span><TrendingUp size={15} /> 증강 성적 분석</span>
          <span><Sparkles size={15} /> AI 맞춤 덱 추천</span>
        </div>
        <button
          type="button"
          className={styles.connectBtn}
          onClick={() => navigate('/')}
        >
          <Link2 size={16} />
          홈에서 소환사 검색으로 연동하기
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  )
}

export default ConnectPrompt
