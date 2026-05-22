import { Clock3 } from 'lucide-react'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck } from '../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../components/common/TierBadge'
import styles from './MetaStats.module.css'

/* ── 티어 순서 및 색상 ── */
const TIER_ORDER: TierBadgeValue[] = ['S', 'A+', 'A', 'B', 'C', 'D']

const TIER_META: Record<TierBadgeValue, { color: string; label: string }> = {
  S:   { color: '#04f3e5', label: '최상위 픽 · 강력 추천' },
  'A+': { color: '#f7d26d', label: '상위권 안정적 덱' },
  A:   { color: '#a78bfa', label: '중상위권 범용 덱' },
  B:   { color: '#60a5fa', label: '중위권 상황 의존적' },
  C:   { color: '#818cf8', label: '하위권 전문 운영 필요' },
  D:   { color: '#6b7280', label: '비추천 · 낮은 안정성' },
}

/* ── 덱 카드 ── */
function DeckCard({ deck }: { deck: MetaDeck }) {
  return (
    <article className={styles.deckCard}>
      <div className={styles.cardTop}>
        <TierBadge value={deck.grade} />
        <span className={styles.cardName}>{deck.name}</span>
        <span className={styles.cardRank}>#{deck.rank}</span>
      </div>
      <div className={styles.cardTraits}>
        {deck.traits.slice(0, 4).map((t) => (
          <TraitHexBadge key={`${t.name}-${t.count}`} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
        ))}
      </div>
      <div className={styles.cardChamps}>
        {deck.champions.slice(0, 6).map((c, i) => (
          <ChampionCard key={`${c.name}-${i}`} imageUrl={c.imageUrl} label={c.name} stars={c.stars} toneIndex={i} />
        ))}
      </div>
      <div className={styles.cardStats}>
        <div className={styles.stat}>
          <small>승률</small>
          <strong className={styles.winRate}>{deck.winRate}</strong>
        </div>
        <div className={styles.stat}>
          <small>TOP 4</small>
          <strong>{deck.top4}</strong>
        </div>
        <div className={styles.stat}>
          <small>평균 등수</small>
          <strong className={styles.avgPlace}>{deck.avgPlace}</strong>
        </div>
        <div className={styles.stat}>
          <small>픽률</small>
          <strong className={styles.pickRate}>{deck.pickRate}</strong>
        </div>
      </div>
    </article>
  )
}

/* ── 티어 섹션 ── */
function TierSection({ tier, decks }: { tier: TierBadgeValue; decks: MetaDeck[] }) {
  if (decks.length === 0) return null
  const { color, label } = TIER_META[tier]

  return (
    <section className={styles.tierSection}>
      <div className={styles.tierHeader} style={{ borderColor: `${color}44` }}>
        <TierBadge value={tier} />
        <span className={styles.tierLabel} style={{ color }}>{tier} 티어</span>
        <span className={styles.tierDesc}>{label}</span>
        <span className={styles.tierCount}>{decks.length}개 덱</span>
      </div>
      <div className={styles.cardGrid}>
        {decks.map((d) => <DeckCard key={d.rank} deck={d} />)}
      </div>
    </section>
  )
}

/* ── 메인 ── */
function MetaStats() {
  const { data: decks = [] } = useMetaSnapshot()

  const byTier = TIER_ORDER.reduce<Record<TierBadgeValue, MetaDeck[]>>(
    (acc, t) => { acc[t] = decks.filter((d) => d.grade === t); return acc },
    { S: [], 'A+': [], A: [], B: [], C: [], D: [] },
  )

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div>
            <h1>메타 통계</h1>
            <p>현재 패치 기준 S~D 티어 전체 덱 분석 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.updateBadge}>
            <Clock3 size={13} />
            <span>업데이트: 3분 전</span>
          </div>
        </div>

        {TIER_ORDER.map((t) => (
          <TierSection key={t} tier={t} decks={byTier[t]} />
        ))}
      </div>
    </AppLayout>
  )
}

export default MetaStats
