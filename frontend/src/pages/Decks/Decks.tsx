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
type SortKey = 'rank' | 'winRate' | 'top4' | 'avgPlace' | 'pickRate'
type SortDir = 'asc' | 'desc'
const TIERS: (TierBadgeValue | 'ALL')[] = ['ALL', 'S', 'A+', 'A', 'B', 'C', 'D']

/* ── 정렬 유틸 ── */
function numVal(s: string) { return parseFloat(s.replace('%', '')) }

function sortDecks(decks: MetaDeck[], key: SortKey, dir: SortDir): MetaDeck[] {
  return [...decks].sort((a, b) => {
    const av = key === 'rank' ? a.rank : numVal(a[key])
    const bv = key === 'rank' ? b.rank : numVal(b[key])
    const asc = key === 'avgPlace' || key === 'rank'
    const base = av < bv ? -1 : av > bv ? 1 : 0
    return (asc ? base : -base) * (dir === 'asc' ? 1 : -1)
  })
}

/* ── 정렬 아이콘 ── */
function SortIcon({ col, cur, dir }: { col: SortKey; cur: SortKey; dir: SortDir }) {
  if (col !== cur) return <ChevronsUpDown size={13} className={styles.sortIcon} />
  return dir === 'asc'
    ? <ChevronUp size={13} className={`${styles.sortIcon} ${styles.sortActive}`} />
    : <ChevronDown size={13} className={`${styles.sortIcon} ${styles.sortActive}`} />
}

/* ── 메인 ── */
function Decks() {
  const { data: decks = [] } = useMetaSnapshot()
  const [tier, setTier] = useState<TierBadgeValue | 'ALL'>('ALL')
  const [search, setSearch] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('rank')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  function handleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir(key === 'avgPlace' ? 'asc' : 'desc')
    }
  }

  const filtered = sortDecks(
    decks
      .filter((d) => tier === 'ALL' || d.grade === tier)
      .filter((d) => d.name.includes(search)),
    sortKey,
    sortDir,
  )

  function SortTh({ label, col }: { label: string; col: SortKey }) {
    return (
      <th className={styles.sortTh} onClick={() => handleSort(col)}>
        {label} <SortIcon col={col} cur={sortKey} dir={sortDir} />
      </th>
    )
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        {/* 헤더 */}
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>현재 패치 기준 전체 메타 덱 · 승률 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.controls}>
            <div className={styles.searchBox}>
              <Search size={14} />
              <input
                placeholder="덱 이름 검색"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
        </div>

        {/* 티어 탭 */}
        <div className={styles.tierTabs}>
          {TIERS.map((t) => (
            <button
              key={t}
              type="button"
              className={tier === t ? styles.activeTab : ''}
              onClick={() => setTier(t)}
            >
              {t === 'ALL' ? '전체' : `${t} 티어`}
            </button>
          ))}
        </div>

        {/* 테이블 */}
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <SortTh label="순위" col="rank" />
                <th>티어</th>
                <th className={styles.nameCol}>덱 이름 / 시너지</th>
                <th className={styles.champCol}>챔피언 구성</th>
                <SortTh label="승률" col="winRate" />
                <SortTh label="TOP 4" col="top4" />
                <SortTh label="평균 등수" col="avgPlace" />
                <SortTh label="픽률" col="pickRate" />
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className={styles.empty}>검색 결과가 없습니다.</td>
                </tr>
              ) : (
                filtered.map((deck) => (
                  <tr key={deck.rank} className={styles.deckRow}>
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
                          <TraitHexBadge
                            key={`${t.name}-${t.count}`}
                            count={t.count}
                            iconUrl={t.iconUrl}
                            name={t.name}
                            tone={t.tone}
                          />
                        ))}
                      </span>
                    </td>
                    <td className={styles.champCol}>
                      <span className={styles.champions}>
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
                      </span>
                    </td>
                    <td className={styles.winRate}>{deck.winRate}</td>
                    <td className={styles.top4}>{deck.top4}</td>
                    <td className={styles.avgPlace}>{deck.avgPlace}</td>
                    <td className={styles.pickRate}>{deck.pickRate}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  )
}

export default Decks
