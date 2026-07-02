import { Clock3 } from 'lucide-react'
import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { TIER_META, TIER_ORDER } from '../../constants/tiers'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { deckDisplayName } from '../Decks/utils/deckListUtils'
import type { TFTLocale } from '../../api/cdragonLocale'
import type { MetaDeck, RankFilter } from '../Dashboard/dashboardData'
import type { RankedTierBadgeValue } from '../../types/badges'
import styles from './MetaStats.module.css'

interface RankFilterOption {
  label: string
  value: RankFilter
}


/* ── 덱 카드 ── */
function DeckCard({ deck, locale }: { deck: MetaDeck; locale: TFTLocale | undefined }) {
  return (
    <article className={styles.deckCard}>
      <div className={styles.cardTop}>
        <TierBadge value={deck.grade} />
        <span className={styles.cardName}>{deckDisplayName(deck, locale)}</span>
      </div>
      <div className={styles.cardTraits}>
        {deck.traits.slice(0, 4).map((t) => (
          <TraitHexBadge key={`${t.name}-${t.count}`} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
        ))}
      </div>
      <div className={styles.cardChamps}>
        {deck.champions.slice(0, 6).map((c, i) => (
          <ChampionCard key={`${c.name}-${i}`} imageUrl={c.imageUrl} label={c.name} stars={c.stars} cost={c.cost} />
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
function TierSection({
  tier,
  decks,
  locale,
}: {
  tier: RankedTierBadgeValue
  decks: MetaDeck[]
  locale: TFTLocale | undefined
}) {
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
        {decks.map((d) => <DeckCard key={d.rank} deck={d} locale={locale} />)}
      </div>
    </section>
  )
}

const RANK_FILTERS: RankFilterOption[] = [
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
  { label: '다이아+',   value: 'DIAMOND_PLUS' },
  { label: '마스터+',   value: 'MASTER_PLUS'  },
]

/* ── 메인 ── */
function MetaStats() {
  const [rankFilter, setRankFilter] = useState<RankFilter>('EMERALD_PLUS')
  const { data: locale } = useCDragonLocale()
  const { data: metaDeckResponse } = useMetaSnapshot(rankFilter)
  const decks = metaDeckResponse?.decks ?? []

  const byTier = TIER_ORDER.reduce<Record<RankedTierBadgeValue, MetaDeck[]>>(
    (acc, t) => { acc[t] = decks.filter((d) => d.grade === t); return acc },
    { S: [], A: [], B: [], C: [], D: [] },
  )

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div>
            <h1>메타 통계</h1>
            <p>현재 패치 기준 S~D 티어 전체 덱 분석 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.headerRight}>
            <div className={styles.rankFilterBar}>
              {RANK_FILTERS.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  className={rankFilter === f.value ? styles.rankFilterActive : styles.rankFilterBtn}
                  aria-pressed={rankFilter === f.value}
                  onClick={() => setRankFilter(f.value)}
                >
                  {f.label}
                </button>
              ))}
            </div>
            <div className={styles.updateBadge}>
              <Clock3 size={13} />
              <span>업데이트: 3분 전</span>
            </div>
          </div>
        </div>

        {TIER_ORDER.map((t) => (
          <TierSection key={t} tier={t} decks={byTier[t]} locale={locale} />
        ))}
      </div>
    </AppLayout>
  )
}

export default MetaStats
