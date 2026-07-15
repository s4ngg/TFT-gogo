import { AlertTriangle, Loader2, RefreshCw } from 'lucide-react'
import styles from '../PatchNotes.module.css'

interface PatchStatusBannerProps {
  isFallbackData: boolean
  isFetching: boolean
  isUnavailableData: boolean
  onRetry: () => void
  patchVersion: string
}

function PatchStatusBanner({
  isFallbackData,
  isFetching,
  isUnavailableData,
  onRetry,
  patchVersion,
}: PatchStatusBannerProps) {
  if (!isFetching && !isFallbackData && !isUnavailableData) return null

  const statusTitle = isFetching
    ? '패치노트 데이터를 불러오는 중입니다.'
    : isUnavailableData
      ? `${patchVersion} 패치 변경사항을 불러오지 못했습니다.`
      : `${patchVersion} 버전 기본 패치노트로 표시 중입니다.`
  const statusDescription = isFetching
    ? '최신 패치노트 응답을 확인하는 동안 같은 버전의 데이터만 표시합니다.'
    : isUnavailableData
      ? '같은 버전의 준비된 데이터가 없어 다른 패치 내용을 대신 표시하지 않습니다.'
      : 'API 응답을 가져오지 못해 같은 버전의 준비된 기본 데이터를 보여주고 있습니다.'

  return (
    <div
      aria-live="polite"
      className={`${styles.statusBanner} ${isFetching ? styles.statusLoading : styles.statusFallback}`}
    >
      <span className={styles.statusIcon}>
        {isFetching ? <Loader2 size={16} /> : <AlertTriangle size={16} />}
      </span>
      <div>
        <strong>{statusTitle}</strong>
        <p>{statusDescription}</p>
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
