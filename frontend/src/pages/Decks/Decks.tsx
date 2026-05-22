import { Search } from 'lucide-react'
import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck } from '../Dashboard/dashboardData'
import styles from './Decks.module.css'

const FILTERS = ['종합', '상위권 (1~4위)', 'S 티어만'] as const
type Filter = typeof FILTERS[number]

function filterDecks(decks: MetaDeck[], filter: Filter, search: string): MetaDeck[] {
  return decks
    .filter((d) => {
      if (filter === '상위권 (1~4위)') return d.rank <= 4
      if (filter === 'S 티어만') return d.grade === 'S'
      return true
    })
    .filter((d) => d.name.includes(search))
}

function Decks() {
  const { data: decks = [] } = useMetaSnapshot()
  const [filter, setFilter] = useState<Filter>('종합')
  const [search, setSearch] = useState('')

  const filtered = filterDecks(decks, filter, search)

  return (
    <AppLayout>
      <div className={styles.page}>
        {/* 헤더 */}
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>현재 패치 기준 메타 덱 전체 목록 · 승률 · TOP4 확인</p>
          </div>
          <div className={styles.controls}>
            <div className={styles.filterTabs}>
              {FILTERS.map((f) => (
                <button
                  key={f}
                  type="button"
                  className={filter === f ? styles.activeTab : ''}
                  onClick={() => setFilter(f)}
                >
                  {f}
                </button>
              ))}
            </div>
            <div className={styles.searchBox}>
              <Search size={15} />
              <input
                placeholder="덱 이름 검색"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* 컬럼 헤더 */}
        <div className={styles.tableHead}>
          <span>순위</span>
          <span>티어</span>
          <span>덱 이름 / 시너지</span>
          <span>챔피언 구성</span>
          <span>승률</span>
          <span>TOP 4</span>
        </div>

        {/* 덱 목록 */}
        <div className={styles.deckList}>
          {filtered.length === 0 ? (
            <div className={styles.empty}>검색 결과가 없습니다.</div>
          ) : (
            filtered.map((deck) => (
              <article key={deck.rank} className={styles.deckRow}>
                <strong className={styles.rank}>{deck.rank}</strong>
                <TierBadge value={deck.grade} />
                <div className={styles.deckInfo}>
                  <h3>{deck.name}</h3>
                  <div className={styles.traits}>
                    {deck.traits.map((t) => (
                      <TraitHexBadge
                        key={`${t.name}-${t.count}`}
                        count={t.count}
                        iconUrl={t.iconUrl}
                        name={t.name}
                        tone={t.tone}
                      />
                    ))}
                  </div>
                </div>
                <div className={styles.champions}>
                  {deck.champions.map((c, i) => (
                    <ChampionCard
                      key={`${c.name}-${i}`}
                      imageUrl={c.imageUrl}
                      items={c.items}
                      label={c.name}
                      stars={c.stars}
                      toneIndex={i}
                    />
                  ))}
                </div>
                <b className={styles.winRate}>{deck.winRate}</b>
                <b className={styles.top4Rate}>{deck.top4}</b>
              </article>
            ))
          )}
        </div>
      </div>
    </AppLayout>
  )
}

export default Decks
