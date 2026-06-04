import { useMemo, useState } from 'react'
import { ChevronRight, Clock3 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../../hooks/useMetaSnapshot'
import type { ChampionSummary, MetaDeck, TraitSummary } from '../dashboardData'
import styles from '../Dashboard.module.css'

type MetaFilter = 'overall' | 'upper' | 'master'
type MetaSortKey = 'top4' | 'avgPlace'

const metaFilters: { label: string; value: MetaFilter }[] = [
  { label: '종합', value: 'overall' },
  { label: '상위권', value: 'upper' },
  { label: '마스터+', value: 'master' },
]

const sortOptions: { label: string; value: MetaSortKey }[] = [
  { label: 'TOP 4', value: 'top4' },
  { label: '평균 등수', value: 'avgPlace' },
]

function toNumber(value: string | undefined): number {
  if (!value) return 0
  return Number(value.replace('%', ''))
}

function filterMetaDecks(decks: MetaDeck[], filter: MetaFilter) {
  if (filter === 'upper') {
    return decks.filter((deck) => deck.grade === 'S' || deck.grade === 'A+')
  }

  if (filter === 'master') {
    return decks.filter((deck) => toNumber(deck.top4) >= 68)
  }

  return decks
}

function sortMetaDecks(decks: MetaDeck[], sortKey: MetaSortKey) {
  const direction = sortKey === 'avgPlace' ? 1 : -1

  return [...decks].sort((a, b) => {
    const result = toNumber(a[sortKey]) - toNumber(b[sortKey])

    if (result === 0) {
      return a.rank - b.rank
    }

    return result * direction
  })
}

interface TraitsProps {
  values: TraitSummary[]
}

function Traits({ values }: TraitsProps) {
  return (
    <div className={styles.traits}>
      {values.map((trait) => (
        <TraitHexBadge
          count={trait.count}
          iconUrl={trait.iconUrl}
          key={`${trait.name}-${trait.count}`}
          name={trait.name}
          tone={trait.tone}
        />
      ))}
    </div>
  )
}

interface ChampionsProps {
  champions: ChampionSummary[]
}

function Champions({ champions }: ChampionsProps) {
  return (
    <div className={styles.champions}>
      {champions.map((champion, index) => (
        <ChampionCard
          imageUrl={champion.imageUrl}
          items={champion.items}
          key={`${champion.name}-${index}`}
          label={champion.name}
          stars={champion.stars}
          cost={champion.cost}
        />
      ))}
    </div>
  )
}

function MetaSnapshot() {
  const { data: metaDeckResponse } = useMetaSnapshot()
  const allDecks = useMemo(() => metaDeckResponse?.decks ?? [], [metaDeckResponse?.decks])
  const [selectedFilter, setSelectedFilter] = useState<MetaFilter>('overall')
  const [sortKey, setSortKey] = useState<MetaSortKey>('top4')
  const metaDecks = useMemo(
    () => sortMetaDecks(filterMetaDecks(allDecks, selectedFilter), sortKey).slice(0, 8),
    [allDecks, selectedFilter, sortKey],
  )
  const navigate = useNavigate()

  return (
    <section className={`${styles.panel} ${styles.metaPanel}`}>
      <div className={styles.panelHeading}>
        <div>
          <h2>추천 메타 스냅샷</h2>
          <span>
            <Clock3 size={17} />
            업데이트: 3분 전
          </span>
        </div>
        <button type="button" onClick={() => navigate('/decks')}>
          전체 보기
          <ChevronRight size={20} />
        </button>
      </div>

      <div className={styles.metaFilters}>
        {metaFilters.map((filter) => (
          <button
            aria-pressed={selectedFilter === filter.value}
            className={selectedFilter === filter.value ? styles.selectedFilter : undefined}
            key={filter.value}
            onClick={() => setSelectedFilter(filter.value)}
            type="button"
          >
            {filter.label}
          </button>
        ))}
        <span aria-hidden="true" className={styles.metaFilterSpacer} />
        {sortOptions.map((option) => (
          <button
            aria-pressed={sortKey === option.value}
            className={`${styles.sortFilter}${sortKey === option.value ? ` ${styles.selectedSort}` : ''}`}
            key={option.value}
            onClick={() => setSortKey(option.value)}
            type="button"
          >
            {option.label}
          </button>
        ))}
      </div>

      <div className={styles.deckList}>
        {metaDecks.length > 0 ? (
          metaDecks.map((deck) => (
            <article className={styles.deckRow} key={deck.rank}>
              <strong className={styles.rankNumber}>{deck.rank}</strong>
              <TierBadge value={deck.grade} />
              <div className={styles.deckInfo}>
                <h3>{deck.name}</h3>
                <Traits values={deck.traits} />
              </div>
              <Champions champions={deck.champions} />
              <b className={styles.top4}>{deck.top4}</b>
              <b className={styles.avgPlace}>{deck.avgPlace}</b>
              <ChevronRight className={styles.rowArrow} size={22} />
            </article>
          ))
        ) : (
          <p className={styles.emptyState}>조건에 맞는 추천 덱이 없습니다.</p>
        )}
      </div>
    </section>
  )
}

export default MetaSnapshot
