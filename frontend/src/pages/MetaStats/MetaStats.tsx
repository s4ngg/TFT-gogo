import { BarChart2, Clock3, TrendingUp, Zap } from 'lucide-react'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck, TraitSummary } from '../Dashboard/dashboardData'
import styles from './MetaStats.module.css'

/* ── 유틸 ── */
function avgWinRate(decks: MetaDeck[]): string {
  if (decks.length === 0) return '0%'
  const sum = decks.reduce((acc, d) => acc + parseFloat(d.winRate), 0)
  return `${(sum / decks.length).toFixed(1)}%`
}

function collectTraitFrequency(decks: MetaDeck[]): { name: string; count: number; iconUrl: string }[] {
  const map = new Map<string, { count: number; iconUrl: string }>()
  for (const deck of decks) {
    for (const t of deck.traits) {
      const prev = map.get(t.name)
      map.set(t.name, { count: (prev?.count ?? 0) + 1, iconUrl: t.iconUrl })
    }
  }
  return Array.from(map.entries())
    .map(([name, v]) => ({ name, ...v }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8)
}

/* ── 서브 컴포넌트 ── */
interface SummaryCardProps {
  label: string
  value: string
  sub?: string
  icon: typeof BarChart2
  accent?: boolean
}

function SummaryCard({ label, value, sub, icon: Icon, accent }: SummaryCardProps) {
  return (
    <div className={`${styles.summaryCard} ${accent ? styles.accentCard : ''}`}>
      <div className={styles.cardIcon}>
        <Icon size={18} />
      </div>
      <div>
        <small>{label}</small>
        <strong>{value}</strong>
        {sub && <span>{sub}</span>}
      </div>
    </div>
  )
}

interface TierBarProps {
  tier: string
  count: number
  total: number
  color: string
}

function TierBar({ tier, count, total, color }: TierBarProps) {
  const pct = total === 0 ? 0 : Math.round((count / total) * 100)
  return (
    <div className={styles.tierBar}>
      <span className={styles.tierLabel} style={{ color }}>{tier}</span>
      <div className={styles.barTrack}>
        <div className={styles.barFill} style={{ width: `${pct}%`, background: color }} />
      </div>
      <span className={styles.tierCount}>{count}개 ({pct}%)</span>
    </div>
  )
}

interface TraitRowProps {
  rank: number
  name: string
  count: number
  max: number
  iconUrl: string
}

function TraitRow({ rank, name, count, max, iconUrl }: TraitRowProps) {
  const pct = max === 0 ? 0 : Math.round((count / max) * 100)
  return (
    <div className={styles.traitRow}>
      <span className={styles.traitRank}>{rank}</span>
      <img className={styles.traitIcon} src={iconUrl} alt={name} />
      <span className={styles.traitName}>{name}</span>
      <div className={styles.traitBarTrack}>
        <div className={styles.traitBarFill} style={{ width: `${pct}%` }} />
      </div>
      <span className={styles.traitCountLabel}>{count}덱</span>
    </div>
  )
}

/* ── 메인 ── */
function MetaStats() {
  const { data: decks = [] } = useMetaSnapshot()

  const tierCounts = {
    S:   decks.filter((d) => d.grade === 'S').length,
    'A+': decks.filter((d) => d.grade === 'A+').length,
    A:   decks.filter((d) => d.grade === 'A').length,
  }
  const traits = collectTraitFrequency(decks)
  const maxTraitCount = traits[0]?.count ?? 1

  const top5 = [...decks].sort((a, b) => parseFloat(b.winRate) - parseFloat(a.winRate)).slice(0, 5)

  return (
    <AppLayout>
      <div className={styles.page}>
        {/* 헤더 */}
        <div className={styles.pageHeader}>
          <div>
            <h1>메타 통계</h1>
            <p>현재 패치 기준 덱 · 시너지 · 티어 분포 분석</p>
          </div>
          <div className={styles.updateBadge}>
            <Clock3 size={13} />
            <span>업데이트: 3분 전</span>
          </div>
        </div>

        {/* 요약 카드 */}
        <div className={styles.summaryRow}>
          <SummaryCard label="분석된 메타 덱" value={`${decks.length}개`} icon={BarChart2} />
          <SummaryCard label="S 티어 덱" value={`${tierCounts.S}개`} sub="최상위 픽률" icon={Zap} accent />
          <SummaryCard label="평균 승률" value={avgWinRate(decks)} icon={TrendingUp} />
          <SummaryCard label="평균 TOP4율" value="69.7%" sub="전체 덱 기준" icon={BarChart2} />
        </div>

        <div className={styles.twoCol}>
          {/* 티어 분포 */}
          <section className={styles.panel}>
            <h2>티어 분포</h2>
            <div className={styles.tierBars}>
              <TierBar tier="S" count={tierCounts.S} total={decks.length} color="#04f3e5" />
              <TierBar tier="A+" count={tierCounts['A+']} total={decks.length} color="#f7c948" />
              <TierBar tier="A" count={tierCounts.A} total={decks.length} color="#a78bfa" />
            </div>
          </section>

          {/* 상위 5덱 승률 */}
          <section className={styles.panel}>
            <h2>승률 TOP 5</h2>
            <div className={styles.top5List}>
              {top5.map((deck, i) => (
                <div key={deck.rank} className={styles.top5Row}>
                  <span className={styles.top5Num}>{i + 1}</span>
                  <TierBadge value={deck.grade} />
                  <span className={styles.top5Name}>{deck.name}</span>
                  <b className={styles.top5WinRate}>{deck.winRate}</b>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* 인기 시너지 */}
        <section className={styles.panel}>
          <h2>인기 시너지 TOP 8 <small>메타 덱 내 등장 빈도</small></h2>
          <div className={styles.traitList}>
            {traits.map((t, i) => (
              <TraitRow
                key={t.name}
                rank={i + 1}
                name={t.name}
                count={t.count}
                max={maxTraitCount}
                iconUrl={t.iconUrl}
              />
            ))}
          </div>
        </section>
      </div>
    </AppLayout>
  )
}

export default MetaStats
