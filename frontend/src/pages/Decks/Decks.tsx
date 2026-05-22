import { ChevronDown, ChevronUp, ChevronsUpDown, Search } from 'lucide-react'
import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck } from '../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../components/common/TierBadge'
import styles from './Decks.module.css'

/* ── 타입 ── */
type Tab = '덱모음' | '메타통계'
type SortKey = 'rank' | 'winRate' | 'top4' | 'avgPlace' | 'pickRate'
type SortDir = 'asc' | 'desc'

const TIER_ORDER: TierBadgeValue[] = ['S', 'A+', 'A', 'B', 'C', 'D']
const TIER_COLOR: Record<TierBadgeValue, string> = {
  S: '#04f3e5', 'A+': '#f7d26d', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

/* ── 정렬 ── */
function numVal(s: string) { return parseFloat(s.replace('%', '')) }
function sortDecks(decks: MetaDeck[], key: SortKey, dir: SortDir) {
  return [...decks].sort((a, b) => {
    const av = key === 'rank' ? a.rank : numVal(a[key])
    const bv = key === 'rank' ? b.rank : numVal(b[key])
    const naturalAsc = key === 'avgPlace' || key === 'rank'
    const base = av < bv ? -1 : av > bv ? 1 : 0
    return (naturalAsc ? base : -base) * (dir === 'asc' ? 1 : -1)
  })
}

/* ── 공통 덱 행 ── */
function DeckRow({ deck }: { deck: MetaDeck }) {
  return (
    <tr className={styles.deckRow}>
      <td>
        <strong className={styles.rank} data-top={deck.rank <= 3 ? deck.rank : undefined}>
          {deck.rank}
        </strong>
      </td>
      <td><TierBadge value={deck.grade} /></td>
      <td className={styles.nameCol}>
        <span className={styles.deckName}>{deck.name}</span>
        <span className={styles.traits}>
          {deck.traits.map((t) => (
            <TraitHexBadge key={`${t.name}-${t.count}`} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
          ))}
        </span>
      </td>
      <td className={styles.champCol}>
        <span className={styles.champions}>
          {deck.champions.map((c, i) => (
            <ChampionCard key={`${c.name}-${i}`} imageUrl={c.imageUrl} items={c.items} label={c.name} stars={c.stars} toneIndex={i} />
          ))}
        </span>
      </td>
      <td className={styles.winRate}>{deck.winRate}</td>
      <td className={styles.top4}>{deck.top4}</td>
      <td className={styles.avgPlace}>{deck.avgPlace}</td>
      <td className={styles.pickRate}>{deck.pickRate}</td>
    </tr>
  )
}

/* ── 공통 테이블 헤더 ── */
function TableHead({ sortKey, sortDir, onSort }: {
  sortKey: SortKey; sortDir: SortDir; onSort: (k: SortKey) => void
}) {
  function Icon({ col }: { col: SortKey }) {
    if (col !== sortKey) return <ChevronsUpDown size={12} className={styles.sortIcon} />
    return sortDir === 'asc'
      ? <ChevronUp size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
      : <ChevronDown size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
  }
  function Th({ label, col }: { label: string; col: SortKey }) {
    return (
      <th className={styles.sortTh} onClick={() => onSort(col)}>
        {label}<Icon col={col} />
      </th>
    )
  }
  return (
    <thead>
      <tr>
        <Th label="순위" col="rank" />
        <th>티어</th>
        <th className={styles.nameCol}>덱 이름 / 시너지</th>
        <th className={styles.champCol}>챔피언 구성</th>
        <Th label="승률" col="winRate" />
        <Th label="TOP 4" col="top4" />
        <Th label="평균 등수" col="avgPlace" />
        <Th label="픽률" col="pickRate" />
      </tr>
    </thead>
  )
}

/* ══ 탭 1: 덱모음 (전체 테이블) ══ */
function DeckListView({ decks }: { decks: MetaDeck[] }) {
  const [search, setSearch] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('rank')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  const filtered = sortDecks(decks.filter((d) => d.name.includes(search)), sortKey, sortDir)

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
          <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} />
          <tbody>
            {filtered.length === 0
              ? <tr><td colSpan={8} className={styles.empty}>검색 결과가 없습니다.</td></tr>
              : filtered.map((d) => <DeckRow key={d.rank} deck={d} />)
            }
          </tbody>
        </table>
      </div>
    </>
  )
}

/* ══ 탭 2: 메타통계 (lolchess 스타일, 티어별 수직 나열) ══ */
function MetaStatsView({ decks }: { decks: MetaDeck[] }) {
  const [sortKey, setSortKey] = useState<SortKey>('rank')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(decks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const color = TIER_COLOR[tier]
            return (
              <>
                {/* 티어 구분 헤더 행 */}
                <tr key={`header-${tier}`} className={styles.tierHeaderRow}>
                  <td colSpan={8}>
                    <span className={styles.tierHeaderInner} style={{ borderLeftColor: color }}>
                      <TierBadge value={tier} />
                      <span className={styles.tierName} style={{ color }}>{tier} 티어</span>
                      <span className={styles.tierDesc}>
                        {tier === 'S' ? '최상위 픽 · 강력 추천'
                          : tier === 'A+' ? '상위권 안정적 덱'
                          : tier === 'A' ? '중상위권 범용 덱'
                          : tier === 'B' ? '중위권 상황 의존적'
                          : tier === 'C' ? '하위권 전문 운영 필요'
                          : '비추천 · 낮은 안정성'}
                      </span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {/* 해당 티어 덱 행 */}
                {tierDecks.map((d) => <DeckRow key={d.rank} deck={d} />)}
              </>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ══ 메인 ══ */
function Decks() {
  const { data: decks = [] } = useMetaSnapshot()
  const [tab, setTab] = useState<Tab>('덱모음')

  return (
    <AppLayout>
      <div className={styles.page}>
        {/* 헤더 + 탭 */}
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>현재 패치 기준 전체 메타 덱 · 승률 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.tabBar}>
            <button
              type="button"
              className={tab === '덱모음' ? styles.activeTab : ''}
              onClick={() => setTab('덱모음')}
            >
              덱모음
            </button>
            <button
              type="button"
              className={tab === '메타통계' ? styles.activeTab : ''}
              onClick={() => setTab('메타통계')}
            >
              메타통계
            </button>
          </div>
        </div>

        {tab === '덱모음'
          ? <DeckListView decks={decks} />
          : <MetaStatsView decks={decks} />
        }
      </div>
    </AppLayout>
  )
}

export default Decks
