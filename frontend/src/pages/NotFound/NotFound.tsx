import { useNavigate } from 'react-router-dom'
import styles from './NotFound.module.css'

function NotFound() {
  const navigate = useNavigate()

  return (
    <div className={styles.container}>
      <div className={styles.code}>404</div>
      <p className={styles.message}>존재하지 않는 페이지입니다.</p>
      <div className={styles.actions}>
        <button className={styles.primaryButton} onClick={() => navigate('/')}>
          홈으로
        </button>
        <button className={styles.secondaryButton} onClick={() => navigate(-1)}>
          이전 페이지
        </button>
      </div>
    </div>
  )
}

export default NotFound
