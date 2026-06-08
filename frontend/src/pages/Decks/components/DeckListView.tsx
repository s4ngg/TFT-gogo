import { Search } from 'lucide-react'
import { useState } from 'react'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { MetaDeck, RankFilter } from '../../Dashboard/dashboardData'
import { sortDecks } from '../utils/deckListUtils'
import type { SortKey, SortDir } from '../utils/deckListUtils'
import DeckRow from './DeckRow'
import TableHead from './TableHead'
import HeroAugmentDeckSection from './HeroAugmentDeckSection'
import styles from '../Decks.module.css'

interface DeckListViewProps {
  decks: MetaDeck[]
  locale: TFTLocale | undefined
  rankFilter: RankFilter
}

function DeckListView({ decks, locale, rankFilter }: DeckListViewProps) {
  const [search, setSearch] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('avgPlace')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  const safeDecks = Array.isArray(decks) ? decks : []
  const filtered = sortDecks(safeDecks.filter((d) => d.name.includes(search)), sortKey, sortDir)

  return (
    <>
      <div className={styles.toolBar}>
        <div className={styles.searchBox}>
          <Search size={14} />
          <input placeholder="덱 이름 검색" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <span className={styles.countLabel}>{filtered.length}개 덱</span>
      </div>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier={false} showRank={false} />
          <tbody>
            {filtered.length === 0
              ? <tr><td colSpan={6} className={styles.empty}>검색 결과가 없습니다.</td></tr>
              : filtered.map((d) => (
                  <DeckRow key={d.rank} deck={d} showTier={false} showRank={false} locale={locale} rankFilter={rankFilter} />
                ))
            }
          </tbody>
        </table>
      </div>
      <HeroAugmentDeckSection locale={locale} />
    </>
  )
}

export default DeckListView
