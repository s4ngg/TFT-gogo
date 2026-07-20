import { useMemo, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../../hooks/useCDragonLocale'
import { deckDisplayName } from '../../Decks/utils/deckListUtils'
import { TIER_ORDER } from '../../../constants/tiers'
import type { ChampionSummary, MetaDeck, RankFilter, TraitSummary } from '../dashboardData'
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

const DETAIL_RANK_FILTER: RankFilter = 'EMERALD_PLUS'

function toNumber(value: string | undefined): number {
  if (!value) return 0
  return Number(value.replace('%', ''))
}

function filterMetaDecks(decks: MetaDeck[], filter: MetaFilter) {
  if (filter === 'upper') {
    return decks.filter((deck) => deck.grade === 'S' || deck.grade === 'A')
  }

  if (filter === 'master') {
    return decks.filter((deck) => toNumber(deck.top4) >= 68)
  }

  return decks
}

function tierRank(grade: MetaDeck['grade']): number {
  const index = TIER_ORDER.indexOf(grade as (typeof TIER_ORDER)[number])
  return index === -1 ? TIER_ORDER.length : index
}

function sortMetaDecks(decks: MetaDeck[], sortKey: MetaSortKey) {
  const direction = sortKey === 'avgPlace' ? 1 : -1

  return [...decks].sort((a, b) => {
    const tierResult = tierRank(a.grade) - tierRank(b.grade)

    if (tierResult !== 0) {
      return tierResult
    }

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
  const { data: metaDeckResponse, isError: isDeckError } = useMetaSnapshot()
  const { data: locale } = useCDragonLocale()
  const allDecks = useMemo(() => metaDeckResponse?.decks ?? [], [metaDeckResponse?.decks])
  const [selectedFilter, setSelectedFilter] = useState<MetaFilter>('overall')
  const [sortKey, setSortKey] = useState<MetaSortKey>('top4')
  const metaDecks = useMemo(
    () => sortMetaDecks(filterMetaDecks(allDecks, selectedFilter), sortKey).slice(0, 8),
    [allDecks, selectedFilter, sortKey],
  )
  const navigate = useNavigate()

  function handleDeckDetailClick(deck: MetaDeck) {
    navigate(`/decks/${DETAIL_RANK_FILTER}/${deck.rank}`)
  }

  return (
    <section className={`${styles.panel} ${styles.metaPanel}`}>
      <div className={styles.panelHeading}>
        <div>
          <h2>추천 메타 스냅샷</h2>
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

      {metaDecks.length > 0 && (
        <div className={styles.deckListHeader}>
          <span />
          <span>덱 정보</span>
          <span>챔피언 구성</span>
          <span>TOP 4</span>
          <span>평균 등수</span>
          <span />
        </div>
      )}

      <div className={styles.deckList}>
        {isDeckError ? (
          <p className={styles.emptyState}>메타 덱 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.</p>
        ) : metaDecks.length > 0 ? (
          metaDecks.map((deck) => (
            <article className={styles.deckRow} key={deck.rank}>
              <TierBadge value={deck.grade} />
              <div className={styles.deckInfo}>
                <h3>{deckDisplayName(deck, locale)}</h3>
                <Traits values={deck.traits} />
              </div>
              <Champions champions={deck.champions} />
              <div className={styles.metricCell} title="TOP 4 진입 비율">
                <span>TOP 4</span>
                <b>{deck.top4}</b>
              </div>
              <div className={styles.metricCell} title="평균 등수">
                <span>평균</span>
                <b>{deck.avgPlace}</b>
              </div>
              <button
                aria-label={`${deckDisplayName(deck, locale)} 상세 보기`}
                className={styles.rowArrow}
                onClick={() => handleDeckDetailClick(deck)}
                type="button"
              >
                <ChevronRight size={20} />
              </button>
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
