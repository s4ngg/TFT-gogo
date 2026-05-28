import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, ChevronsUpDown, Search } from 'lucide-react'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck } from '../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../components/common/TierBadge'
import { ARTIFACT_RECS, HERO_AUGMENT_DECKS, INITIAL_ARTIFACT_COUNT } from './deckListData'
import styles from './Decks.module.css'

/* ════════════════════════════
   타입
════════════════════════════ */
type Tab = '덱모음' | '메타통계'
type SortKey = 'rank' | 'winRate' | 'top4' | 'avgPlace' | 'pickRate'
type SortDir = 'asc' | 'desc'

const TIER_ORDER: TierBadgeValue[] = ['S', 'A+', 'A', 'B', 'C', 'D']
const TIER_COLOR: Record<TierBadgeValue, string> = {
  S: '#04f3e5', 'A+': '#f7d26d', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

/* ════════════════════════════
   유틸
════════════════════════════ */
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

/* ════════════════════════════
   공통 컴포넌트
════════════════════════════ */
function SortIcon({ col, cur, dir }: { col: SortKey; cur: SortKey; dir: SortDir }) {
  if (col !== cur) return <ChevronsUpDown size={12} className={styles.sortIcon} />
  return dir === 'asc'
    ? <ChevronUp   size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
    : <ChevronDown size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
}

/** 덱 행 */
function DeckRow({
  deck, showTier = true, showRank = true,
}: { deck: MetaDeck; showTier?: boolean; showRank?: boolean }) {
  const navigate = useNavigate()
  function handleRowClick() { navigate(`/decks/${deck.rank}`) }
  return (
    <tr className={styles.deckRow} onClick={handleRowClick}>
      {showRank && (
        <td>
          <strong className={styles.rank} data-top={deck.rank <= 3 ? deck.rank : undefined}>
            {deck.rank}
          </strong>
        </td>
      )}
      {showTier && <td><TierBadge value={deck.grade} /></td>}
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
            <ChampionCard key={`${c.name}-${i}`} imageUrl={c.imageUrl} items={c.items} label={c.name} stars={c.stars} cost={c.cost} />
          ))}
        </span>
      </td>
      <td className={styles.winRate}>{deck.winRate}</td>
      <td className={styles.top4}>{deck.top4}</td>
      <td className={styles.avgPlace}><span className={styles.avgHash}>#</span>{deck.avgPlace}</td>
      <td className={styles.pickRate}>{deck.pickRate}</td>
    </tr>
  )
}

/** 테이블 헤더 */
function TableHead({ sortKey, sortDir, onSort, showTier = true, showRank = true }: {
  sortKey: SortKey; sortDir: SortDir; onSort: (k: SortKey) => void
  showTier?: boolean; showRank?: boolean
}) {
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
        <Th label="승률" col="winRate" />
        <Th label="TOP 4" col="top4" />
        <Th label="평균 등수" col="avgPlace" />
        <Th label="픽률" col="pickRate" />
      </tr>
    </thead>
  )
}

/* ════════════════════════════
   영웅 증강 섹션 (캐러셀)
════════════════════════════ */
function HeroAugmentSection() {
  const scrollRef = useRef<HTMLDivElement>(null)

  function scrollCarousel(dir: 'left' | 'right') {
    if (!scrollRef.current) return
    scrollRef.current.scrollBy({ left: dir === 'left' ? -316 : 316, behavior: 'smooth' })
  }

  // 추천(true) → 왼쪽, 비추천(false) → 오른쪽
  const sorted = [...HERO_AUGMENT_DECKS].sort((a, b) => {
    if (a.recommended === b.recommended) return 0
    return a.recommended ? -1 : 1
  })

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={styles.specialBadge}>영웅 증강</span>
        <div className={styles.specialHeaderText}>
          <h2>영웅 증강 특수 덱</h2>
          <p>특정 영웅 증강을 보유했을 때만 가능한 고승률 전략</p>
        </div>
        <div className={styles.carouselBtns}>
          <button type="button" className={styles.carouselBtn} onClick={() => scrollCarousel('left')}>
            <ChevronLeft size={16} />
          </button>
          <button type="button" className={styles.carouselBtn} onClick={() => scrollCarousel('right')}>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <div className={styles.augmentCarousel} ref={scrollRef}>
        {sorted.map((d) => (
          <article
            key={`${d.hero}-${d.augment}`}
            className={styles.augmentCard}
            data-recommended={d.recommended ? 'true' : 'false'}
          >
            <div className={styles.augCardTop}>
              <div>
                <div className={styles.augHeroName}>{d.hero}</div>
                <div className={styles.augName}>[{d.augment}]</div>
              </div>
              <span className={d.recommended ? styles.augRecommendBadge : styles.augNotRecommendBadge}>
                {d.recommended ? '추천' : '비추천'}
              </span>
            </div>
            <p className={styles.augDesc}>{d.description}</p>
            <div className={styles.augTags}>
              {d.tags.map((tag) => <span key={tag} className={styles.augTag}>{tag}</span>)}
            </div>
            <div className={styles.augChamps}>
              {d.champions.map((c, i) => (
                <ChampionCard key={c.name} imageUrl={c.imageUrl} label={c.name} stars={c.stars} cost={c.cost} />
              ))}
            </div>
            <div className={styles.augStats}>
              <div><small>승률</small><strong className={styles.winRate}>{d.winRate}</strong></div>
              <div><small>평균 등수</small><strong className={styles.avgPlace}><span className={styles.avgHash}>#</span>{d.avgPlace}</strong></div>
              <div><small>픽률</small><strong className={styles.pickRate}>{d.pickRate}</strong></div>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

/* ════════════════════════════
   유물 섹션 (검색 상시 노출 + 더보기)
════════════════════════════ */
function ArtifactSection() {
  const [showAll, setShowAll] = useState(false)
  const [search, setSearch]   = useState('')

  const searchActive = search.trim() !== ''
  // 검색 중이면 전체에서 필터, 아니면 showAll 여부에 따라 slice
  const allFiltered = ARTIFACT_RECS.filter((r) => !searchActive || r.itemName.includes(search))
  const visible     = searchActive || showAll ? allFiltered : allFiltered.slice(0, INITIAL_ARTIFACT_COUNT)
  const hiddenCount = ARTIFACT_RECS.length - INITIAL_ARTIFACT_COUNT

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={`${styles.specialBadge} ${styles.artifactBadge}`}>
          유물 추천
        </span>
        <div className={styles.specialHeaderText}>
          <h2>유물별 최적 유닛</h2>
          <p>시너지 무관, 해당 유물 장착 시 승률이 크게 오르는 유닛</p>
        </div>
        {/* 검색 — 항상 표시 */}
        <div className={styles.artifactSearch}>
          <Search size={14} />
          <input
            placeholder="유물 이름 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className={styles.artifactList}>
        {visible.length === 0 ? (
          <p className={styles.empty}>검색 결과가 없습니다.</p>
        ) : (
          visible.map((rec) => (
            <div key={rec.itemName} className={styles.artifactRow}>
              {/* 아이템 아이콘 + 이름 */}
              <div className={styles.artifactItem}>
                <img src={rec.itemIcon} alt={rec.itemName} className={styles.artifactIcon} />
                <span className={styles.artifactName}>{rec.itemName}</span>
              </div>

              {/* 유닛 목록 + 컬럼 헤더 */}
              <div className={styles.artifactUnits}>
                <div className={styles.artifactUnitHeader}>
                  <span />
                  <span>유닛</span>
                  <span>빈도수</span>
                  <span>승률</span>
                  <span>평균 등수 향상</span>
                  <span>TOP4</span>
                </div>
                {rec.units.map((u) => (
                  <div key={u.name} className={styles.artifactUnit}>
                    <img src={u.imageUrl} alt={u.name} className={styles.artifactChampImg} />
                    <span className={styles.artifactUnitName}>{u.name}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatFreq}`}>{u.frequency}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatWin}`}>{u.winRate}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatImp}`}>{u.avgImprovement}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatTop4}`}>{u.top4}</span>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 더보기 / 접기 — 검색 중이 아닐 때만 표시 */}
      {!searchActive && hiddenCount > 0 && (
        <button
          type="button"
          className={styles.showMoreBtn}
          onClick={() => setShowAll((v) => !v)}
        >
          {showAll ? '접기' : `더보기 (${hiddenCount}개 더)`}
        </button>
      )}
    </section>
  )
}

/* ════════════════════════════
   탭 1 — 덱모음
   (순위·티어 없음 + 영웅 증강 + 유물 추천)
════════════════════════════ */
function DeckListView({ decks }: { decks: MetaDeck[] }) {
  const [search, setSearch]   = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('winRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

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
          <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier={false} showRank={false} />
          <tbody>
            {filtered.length === 0
              ? <tr><td colSpan={6} className={styles.empty}>검색 결과가 없습니다.</td></tr>
              : filtered.map((d) => <DeckRow key={d.rank} deck={d} showTier={false} showRank={false} />)
            }
          </tbody>
        </table>
      </div>
      <HeroAugmentSection />
      <ArtifactSection />
    </>
  )
}

/* ════════════════════════════
   탭 2 — 메타통계
   (lolchess 스타일, 티어별 수직)
════════════════════════════ */
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
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier showRank />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(decks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const color = TIER_COLOR[tier]
            return (
              <>
                <tr key={`header-${tier}`} className={styles.tierHeaderRow}>
                  <td colSpan={8}>
                    <span className={styles.tierHeaderInner} style={{ borderLeftColor: color }}>
                      <TierBadge value={tier} />
                      <span className={styles.tierName} style={{ color }}>{tier} 티어</span>
                      <span className={styles.tierDesc}>
                        {tier === 'S'  ? '최상위 픽 · 강력 추천'
                          : tier === 'A+' ? '상위권 안정적 덱'
                          : tier === 'A'  ? '중상위권 범용 덱'
                          : tier === 'B'  ? '중위권 상황 의존적'
                          : tier === 'C'  ? '하위권 전문 운영 필요'
                          : '비추천 · 낮은 안정성'}
                      </span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {tierDecks.map((d) => <DeckRow key={d.rank} deck={d} showTier showRank />)}
              </>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ════════════════════════════
   메인
════════════════════════════ */
function Decks() {
  const { data: decks = [] } = useMetaSnapshot()
  const [tab, setTab] = useState<Tab>('덱모음')

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>현재 패치 기준 전체 메타 덱 · 승률 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.tabBar}>
            <button type="button" className={tab === '덱모음' ? styles.activeTab : ''} onClick={() => setTab('덱모음')}>
              덱모음
            </button>
            <button type="button" className={tab === '메타통계' ? styles.activeTab : ''} onClick={() => setTab('메타통계')}>
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
