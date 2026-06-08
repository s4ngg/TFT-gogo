import { ChevronDown, ChevronUp, ChevronsUpDown } from 'lucide-react'
import type { SortKey, SortDir } from '../utils/deckListUtils'
import styles from '../Decks.module.css'

interface SortIconProps { col: SortKey; cur: SortKey; dir: SortDir }

function SortIcon({ col, cur, dir }: SortIconProps) {
  if (col !== cur) return <ChevronsUpDown size={12} className={styles.sortIcon} />
  return dir === 'asc'
    ? <ChevronUp size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
    : <ChevronDown size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
}

interface TableHeadProps {
  sortKey: SortKey
  sortDir: SortDir
  onSort: (k: SortKey) => void
  showTier?: boolean
  showRank?: boolean
}

function TableHead({ sortKey, sortDir, onSort, showTier = true, showRank = true }: TableHeadProps) {
  function Th({ label, col }: { label: string; col: SortKey }) {
    return (
      <th className={styles.sortTh} onClick={() => onSort(col)}>
        {label}<SortIcon col={col} cur={sortKey} dir={sortDir} />
      </th>
    )
  }

  return (
    <thead>
      <tr>
        {showRank && <Th label="순위" col="rank" />}
        {showTier && <th>티어</th>}
        <th className={styles.nameCol}>덱 이름 / 시너지</th>
        <th className={styles.champCol}>챔피언 구성</th>
        <Th label="TOP 4" col="top4" />
        <Th label="평균 등수" col="avgPlace" />
        <Th label="픽률" col="pickRate" />
      </tr>
    </thead>
  )
}

export default TableHead
