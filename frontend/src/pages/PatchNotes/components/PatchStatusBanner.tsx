import { AlertTriangle, Loader2, RefreshCw } from 'lucide-react'
import styles from '../PatchNotes.module.css'

interface PatchStatusBannerProps {
  isFallbackData: boolean
  isFetching: boolean
  onRetry: () => void
}

function PatchStatusBanner({ isFallbackData, isFetching, onRetry }: PatchStatusBannerProps) {
  if (!isFetching && !isFallbackData) return null

  return (
    <div
      aria-live="polite"
      className={`${styles.statusBanner} ${isFetching ? styles.statusLoading : styles.statusFallback}`}
    >
      <span className={styles.statusIcon}>
        {isFetching ? <Loader2 size={16} /> : <AlertTriangle size={16} />}
      </span>
      <div>
        <strong>{isFetching ? '패치노트 데이터를 불러오는 중입니다.' : '기본 패치노트로 표시 중입니다.'}</strong>
        <p>
          {isFetching
            ? '최신 패치노트 응답을 확인하는 동안 현재 데이터를 유지합니다.'
            : '패치노트 API 응답을 가져오지 못해 준비된 기본 데이터를 보여주고 있습니다.'}
        </p>
      </div>
      {!isFetching && (
        <button onClick={onRetry} type="button">
          <RefreshCw size={14} />
          다시 시도
        </button>
      )}
    </div>
  )
}

export default PatchStatusBanner
