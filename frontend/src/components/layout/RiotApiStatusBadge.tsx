import { Activity, AlertTriangle, RadioTower, TimerReset } from 'lucide-react'
import { useRiotApiStatus, type RiotApiStatus } from '../../hooks/useRiotApiStatus'
import type { RiotApiStatusKind } from '../../api/riotApi'
import styles from './Layout.module.css'

const statusLabelMap: Record<RiotApiStatusKind, string> = {
  available: '연동 정상',
  queue: '대기열',
  limited: '제한 감지',
  degraded: 'Fallback',
}

const statusClassMap: Record<RiotApiStatusKind, string> = {
  available: styles.riotStatusAvailable,
  queue: styles.riotStatusQueue,
  limited: styles.riotStatusLimited,
  degraded: styles.riotStatusDegraded,
}

const statusIconMap: Record<RiotApiStatusKind, typeof Activity> = {
  available: RadioTower,
  queue: TimerReset,
  limited: AlertTriangle,
  degraded: AlertTriangle,
}

const numberFormatter = new Intl.NumberFormat('ko-KR')

function buildStatusLabel(status: RiotApiStatus) {
  const sourceLabel = status.source === 'fallback' ? 'fallback 표시' : '실시간 응답'
  const metricLabel =
    status.source === 'fallback'
      ? '운영 수치 확인 불가'
      : `처리 중 요청 ${numberFormatter.format(status.activeConnections)}건, 대기 요청 ${numberFormatter.format(
          status.queueSize,
        )}건`

  return `Riot API 상태: ${statusLabelMap[status.status]}, ${metricLabel}, ${sourceLabel}`
}

function RiotApiStatusBadge() {
  const { data: riotStatus } = useRiotApiStatus()
  const status = riotStatus

  if (!status) {
    return null
  }

  const Icon = statusIconMap[status.status]

  return (
    <div
      aria-label={buildStatusLabel(status)}
      className={`${styles.riotStatus} ${statusClassMap[status.status]}`}
      title={status.message}
    >
      <Icon size={15} strokeWidth={2.2} />
      <strong>{statusLabelMap[status.status]}</strong>
      {status.source === 'fallback' ? (
        <span>수치 확인 불가</span>
      ) : (
        <>
          <span>요청 {numberFormatter.format(status.activeConnections)}</span>
          <span>대기 {numberFormatter.format(status.queueSize)}</span>
        </>
      )}
    </div>
  )
}

export default RiotApiStatusBadge
