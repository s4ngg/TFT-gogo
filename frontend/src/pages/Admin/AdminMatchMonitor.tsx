import { useQuery } from '@tanstack/react-query'
import { RefreshCcw } from 'lucide-react'
import { fetchMatchCacheStats, fetchRateLimitStats } from '../../api/adminApi'
import RateLimitGauge from './components/RateLimitGauge'
import styles from './Admin.module.css'
import monStyles from './AdminMatchMonitor.module.css'

function fmtTimestamp(ts: number | null): string {
  if (ts == null) return '-'
  return new Date(ts).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  })
}

function fmtMs(ms: number): string {
  return `${Math.ceil(ms / 1000)}s`
}

function AdminMatchMonitor() {
  const {
    data: rateLimit,
    isLoading: rateLimitLoading,
    isError: rateLimitError,
  } = useQuery({
    queryKey: ['admin', 'rate-limit'],
    queryFn: fetchRateLimitStats,
    refetchInterval: 3000,
  })

  const {
    data: cache,
    isLoading: cacheLoading,
    isError: cacheError,
    refetch: refetchCache,
  } = useQuery({
    queryKey: ['admin', 'match-cache-stats'],
    queryFn: fetchMatchCacheStats,
  })

  const shortUsed = rateLimit ? rateLimit.shortMax - rateLimit.shortRemaining : 0
  const longUsed = rateLimit ? rateLimit.longMax - rateLimit.longRemaining : 0

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>전적 모니터링</h1>

      {/* ── Rate Limit ── */}
      <section className={monStyles.section}>
        <div className={monStyles.sectionHeader}>
          <h2 className={monStyles.sectionTitle}>Riot API Rate Limit</h2>
          <span className={monStyles.autoTag}>3초 자동 갱신</span>
        </div>

        {rateLimitError && <p className={monStyles.error}>Rate Limit 정보 불러오기 실패</p>}

        {rateLimitLoading && <p className={monStyles.empty}>불러오는 중...</p>}

        {rateLimit && (
          <div className={monStyles.gaugeList}>
            <RateLimitGauge
              used={shortUsed}
              max={rateLimit.shortMax}
              label={`단기 버킷 (${rateLimit.shortWindowMs / 1000}s 윈도우)`}
              sub={`잔여 토큰 ${rateLimit.shortRemaining}개 · 창 리셋까지 ${fmtMs(rateLimit.shortWindowRemainMs)}`}
            />
            <RateLimitGauge
              used={longUsed}
              max={rateLimit.longMax}
              label={`장기 버킷 (${rateLimit.longWindowMs / 1000}s 윈도우)`}
              sub={`잔여 토큰 ${rateLimit.longRemaining}개 · 창 리셋까지 ${fmtMs(rateLimit.longWindowRemainMs)}`}
            />
          </div>
        )}
      </section>

      {/* ── 캐시 현황 ── */}
      <section className={monStyles.section}>
        <div className={monStyles.sectionHeader}>
          <h2 className={monStyles.sectionTitle}>매치 캐시 현황</h2>
          <button
            type="button"
            className={styles.saveBtn}
            onClick={() => refetchCache()}
            disabled={cacheLoading}
          >
            <RefreshCcw size={13} className={monStyles.iconInline} />
            {cacheLoading ? '조회 중...' : '새로고침'}
          </button>
        </div>

        {cacheError && <p className={monStyles.error}>캐시 통계 불러오기 실패</p>}

        {cache && (
          <div className={monStyles.statGrid}>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>전체 캐시</span>
              <span className={monStyles.statValue}>{cache.totalCount.toLocaleString()}</span>
              <span className={monStyles.statUnit}>게임</span>
            </div>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>랭크 (1100)</span>
              <span className={monStyles.statValue}>{cache.rankedCount.toLocaleString()}</span>
              <span className={monStyles.statUnit}>게임</span>
            </div>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>일반 (1090)</span>
              <span className={monStyles.statValue}>{cache.normalCount.toLocaleString()}</span>
              <span className={monStyles.statUnit}>게임</span>
            </div>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>최신 매치</span>
              <span className={`${monStyles.statValue} ${monStyles.statValueSm}`}>
                {fmtTimestamp(cache.newestMatchTimestamp)}
              </span>
            </div>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>가장 오래된 매치</span>
              <span className={`${monStyles.statValue} ${monStyles.statValueSm}`}>
                {fmtTimestamp(cache.oldestMatchTimestamp)}
              </span>
            </div>
            <div className={monStyles.statCard}>
              <span className={monStyles.statLabel}>마지막 캐시 저장</span>
              <span className={`${monStyles.statValue} ${monStyles.statValueSm}`}>
                {cache.lastCachedAt ? fmtTimestamp(new Date(cache.lastCachedAt).getTime()) : '-'}
              </span>
            </div>
          </div>
        )}

        {!cache && !cacheLoading && !cacheError && (
          <p className={monStyles.empty}>불러오는 중...</p>
        )}
      </section>
    </div>
  )
}

export default AdminMatchMonitor
