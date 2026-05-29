import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, ChevronsUpDown, Search } from 'lucide-react'
import { useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { tftItemIconUrl } from '../../api/communityDragonAssets'
import { getChampionName, getChampionShortName, getTraitName, getItemName, getAugmentName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import type { MetaDeck, RankFilter } from '../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../components/common/TierBadge'
import styles from './Decks.module.css'

const INITIAL_ITEM_COUNT = 4

/* ════════════════════════════
   타입
════════════════════════════ */
type Tab = '덱모음' | '메타통계'
type SortKey = 'rank' | 'winRate' | 'top4' | 'avgPlace' | 'pickRate'
type SortDir = 'asc' | 'desc'

interface RankFilterOption {
  label: string
  value: RankFilter
}

const TIER_ORDER: TierBadgeValue[] = ['S', 'A', 'B', 'C', 'D']
const TIER_COLOR: Record<TierBadgeValue, string> = {
  S: '#04f3e5', 'A+': '#f7d26d', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

const NON_SHOP_CHAMPION_NAMES = new Set(['ElderDragon', 'IvernMinion', 'Summon'])

/* ════════════════════════════
   유틸
════════════════════════════ */
function numVal(s: string) { return parseFloat(s.replace('%', '')) }

function deckDisplayName(deck: MetaDeck, locale: TFTLocale | undefined): string {
  // 주요 시너지 (1개) + 캐리 챔피언명 (최대 2명) — lolchess 스타일
  const traitName = deck.traits.length > 0
    ? getTraitName(deck.traits[0].name, locale)
    : ''
  const carries = deck.champions
    .filter((c) => (c.recommendedItems?.length ?? 0) > 0)
    .slice(0, 2)
    .map((c) => getChampionShortName(c.imageUrl, locale, c.name))
  const parts = [traitName, ...carries].filter(Boolean)
  return parts.length > 0 ? parts.join(' ') : deck.name
}

function shopChampions(deck: MetaDeck) {
  return deck.champions.filter((champion) => !NON_SHOP_CHAMPION_NAMES.has(champion.name))
}

function isArtifactItem(itemId: string) {
  const id = itemId.toLowerCase()
  return id.includes('artifact') || id.includes('ornn') || id.includes('shimmerscale')
}

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
  deck, showTier = true, showRank = true, locale,
}: { deck: MetaDeck; showTier?: boolean; showRank?: boolean; locale: TFTLocale | undefined }) {
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
        <span className={styles.deckName}>{deckDisplayName(deck, locale)}</span>
        <span className={styles.traits}>
          {deck.traits.map((t) => (
            <TraitHexBadge
              key={`${t.name}-${t.count}`}
              count={t.count}
              iconUrl={t.iconUrl}
              name={getTraitName(t.name, locale)}
              tone={t.tone}
            />
          ))}
        </span>
      </td>
      <td className={styles.champCol}>
        <span className={styles.champions}>
          {shopChampions(deck).slice(0, 9).map((c, i) => (
            <ChampionCard
              key={`${c.name}-${i}`}
              imageUrl={c.imageUrl}
              items={c.items}
              label={getChampionName(c.imageUrl, locale, c.name)}
              stars={c.stars}
              cost={c.cost}
            />
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
   영웅 증강 섹션 (캐러셀) — 실데이터
════════════════════════════ */
function HeroAugmentSection({ decks, locale }: { decks: MetaDeck[]; locale: TFTLocale | undefined }) {
  const scrollRef = useRef<HTMLDivElement>(null)

  function scrollCarousel(dir: 'left' | 'right') {
    if (!scrollRef.current) return
    scrollRef.current.scrollBy({ left: dir === 'left' ? -316 : 316, behavior: 'smooth' })
  }

  // 증강 데이터 있는 덱만 표시, 승률 내림차순 정렬
  const sorted = [...decks]
    .filter((d) => (d.topAugments?.length ?? 0) > 0)
    .sort((a, b) => numVal(b.winRate) - numVal(a.winRate))

  const isEmpty = sorted.length === 0

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={styles.specialBadge}>영웅 증강</span>
        <div className={styles.specialHeaderText}>
          <h2>덱별 추천 증강</h2>
          <p>각 메타 덱에서 가장 높은 승률을 기록한 증강 조합</p>
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

      {isEmpty ? (
        <p className={styles.empty}>집계 완료 후 증강 데이터가 표시됩니다</p>
      ) : (
        <div className={styles.augmentCarousel} ref={scrollRef}>
          {sorted.map((deck) => {
            const topAug = deck.topAugments![0]
            const isRec = topAug.isRecommended
            const augName = getAugmentName(topAug.augmentId, locale)
            return (
              <article
                key={deck.rank}
                className={styles.augmentCard}
                data-recommended={isRec ? 'true' : 'false'}
              >
                <div className={styles.augCardTop}>
                  <div>
                    <div className={styles.augHeroName}>{deckDisplayName(deck, locale)}</div>
                    <div className={styles.augName}>[{augName}]</div>
                  </div>
                  <span className={isRec ? styles.augRecommendBadge : styles.augNotRecommendBadge}>
                    {isRec ? '추천' : '비추천'}
                  </span>
                </div>
                <div className={styles.augTags}>
                  {deck.topAugments!.slice(0, 3).map((a) => (
                    <span key={a.augmentId} className={styles.augTag}>
                      {getAugmentName(a.augmentId, locale)} ({a.winRate})
                    </span>
                  ))}
                </div>
                <div className={styles.augChamps}>
                  {shopChampions(deck).slice(0, 4).map((c, i) => (
                    <ChampionCard
                      key={`${c.name}-${i}`}
                      imageUrl={c.imageUrl}
                      label={getChampionName(c.imageUrl, locale, c.name)}
                      stars={c.stars}
                      cost={c.cost}
                    />
                  ))}
                </div>
                <div className={styles.augStats}>
                  <div><small>승률</small><strong className={styles.winRate}>{deck.winRate}</strong></div>
                  <div><small>평균 등수</small><strong className={styles.avgPlace}><span className={styles.avgHash}>#</span>{deck.avgPlace}</strong></div>
                  <div><small>픽률</small><strong className={styles.pickRate}>{deck.pickRate}</strong></div>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

/* ════════════════════════════
   유물 섹션 — 실데이터 (아이템 → 유닛 pivot)
════════════════════════════ */
interface ItemUnitEntry {
  itemId: string
  itemName: string
  iconUrl: string
  placementDelta: number   // 양수일수록 등수 향상 효과가 큼
  winRate: string
  units: { name: string; imageUrl: string }[]
}

function buildItemUnitEntries(decks: MetaDeck[]): ItemUnitEntry[] {
  const map = new Map<string, {
    placementDelta: number
    winRate: string
    iconUrl: string
    itemName: string
    unitSet: Map<string, { name: string; imageUrl: string }>
  }>()

  decks.forEach((deck) => {
    deck.topItems?.forEach((item) => {
      if (!isArtifactItem(item.itemId)) return
      const delta = parseFloat(item.placementDelta)
      if (!Number.isFinite(delta) || delta <= 0) return

      if (!map.has(item.itemId)) {
        map.set(item.itemId, {
          placementDelta: delta,
          winRate: item.winRate,
          iconUrl: tftItemIconUrl(item.itemId),
          itemName: item.itemName,
          unitSet: new Map(),
        })
      }
      const entry = map.get(item.itemId)!

      shopChampions(deck).forEach((champ) => {
        if (champ.recommendedItems?.includes(item.itemId) && !entry.unitSet.has(champ.name)) {
          entry.unitSet.set(champ.name, { name: champ.name, imageUrl: champ.imageUrl })
        }
      })
    })
  })

  return Array.from(map.entries())
    .map(([itemId, { placementDelta, winRate, iconUrl, itemName, unitSet }]) => ({
      itemId,
      itemName,
      iconUrl,
      placementDelta,
      winRate,
      units: Array.from(unitSet.values()),
    }))
    .filter((e) => e.units.length > 0)
    .sort((a, b) => b.placementDelta - a.placementDelta)
}

function ArtifactSection({ decks, locale }: { decks: MetaDeck[]; locale: TFTLocale | undefined }) {
  const [showAll, setShowAll] = useState(false)
  const [search, setSearch]   = useState('')

  const entries = useMemo(() => buildItemUnitEntries(decks), [decks])
  const hasData = entries.length > 0

  const searchActive = search.trim() !== ''
  const allFiltered  = entries.filter((e) => {
    if (!searchActive) return true
    const displayName = getItemName(e.itemId, locale, e.itemName).toLowerCase()
    return displayName.includes(search.toLowerCase())
  })
  const visible     = searchActive || showAll ? allFiltered : allFiltered.slice(0, INITIAL_ITEM_COUNT)
  const hiddenCount = allFiltered.length - INITIAL_ITEM_COUNT

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={`${styles.specialBadge} ${styles.artifactBadge}`}>
          아이템 추천
        </span>
        <div className={styles.specialHeaderText}>
          <h2>아이템별 최적 유닛</h2>
          <p>해당 아이템 장착 시 승률이 크게 오르는 유닛 (집계 실데이터)</p>
        </div>
        <div className={styles.artifactSearch}>
          <Search size={14} />
          <input
            placeholder="아이템 이름 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {!hasData ? (
        <p className={styles.empty}>집계 완료 후 아이템 데이터가 표시됩니다</p>
      ) : (
        <>
          <div className={styles.artifactList}>
            {visible.length === 0 ? (
              <p className={styles.empty}>검색 결과가 없습니다.</p>
            ) : (
              visible.map((entry) => (
                <div key={entry.itemId} className={styles.artifactRow}>
                  <div className={styles.artifactItem}>
                    <img
                      src={entry.iconUrl}
                      alt={entry.itemName}
                      className={styles.artifactIcon}
                      onError={(e) => { (e.currentTarget as HTMLImageElement).style.opacity = '0.3' }}
                    />
                    <div className={styles.artifactItemInfo}>
                      <span className={styles.artifactName}>{getItemName(entry.itemId, locale, entry.itemName)}</span>
                      <span className={styles.artifactDelta}>
                        등수 향상 +{entry.placementDelta.toFixed(2)}
                      </span>
                    </div>
                  </div>

                  <div className={styles.artifactUnits}>
                    {entry.units.slice(0, 4).map((u) => (
                      <div key={u.name} className={styles.artifactUnit}>
                        <img src={u.imageUrl} alt={u.name} className={styles.artifactChampImg} />
                        <span className={styles.artifactUnitName}>{getChampionName(u.imageUrl, locale, u.name)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))
            )}
          </div>

          {!searchActive && hiddenCount > 0 && (
            <button
              type="button"
              className={styles.showMoreBtn}
              onClick={() => setShowAll((v) => !v)}
            >
              {showAll ? '접기' : `더보기 (${hiddenCount}개 더)`}
            </button>
          )}
        </>
      )}
    </section>
  )
}

/* ════════════════════════════
   탭 1 — 덱모음
   (순위·티어 없음 + 영웅 증강 + 유물 추천)
════════════════════════════ */
function DeckListView({ decks, locale }: { decks: MetaDeck[]; locale: TFTLocale | undefined }) {
  const [search, setSearch]   = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('winRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

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
              : filtered.map((d) => <DeckRow key={d.rank} deck={d} showTier={false} showRank={false} locale={locale} />)
            }
          </tbody>
        </table>
      </div>
      <HeroAugmentSection decks={safeDecks} locale={locale} />
      <ArtifactSection decks={safeDecks} locale={locale} />
    </>
  )
}

/* ════════════════════════════
   탭 2 — 메타통계
   (lolchess 스타일, 티어별 수직)
════════════════════════════ */
function MetaStatsView({ decks, locale }: { decks: MetaDeck[]; locale: TFTLocale | undefined }) {
  const [sortKey, setSortKey] = useState<SortKey>('pickRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  const safeDecks = Array.isArray(decks) ? decks : []

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier showRank={false} />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(safeDecks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const color = TIER_COLOR[tier]
            return (
              <>
                <tr key={`header-${tier}`} className={styles.tierHeaderRow}>
                  <td colSpan={7}>
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
                {tierDecks.map((d) => <DeckRow key={d.rank} deck={d} showTier showRank={false} locale={locale} />)}
              </>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

const RANK_FILTERS: RankFilterOption[] = [
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
  { label: '다이아+',   value: 'DIAMOND_PLUS' },
  { label: '마스터+',   value: 'MASTER_PLUS'  },
]

/* ════════════════════════════
   메인
════════════════════════════ */
function Decks() {
  const [rankFilter, setRankFilter] = useState<RankFilter>('EMERALD_PLUS')
  const { data: metaDeckResponse } = useMetaSnapshot(rankFilter)
  const decks = metaDeckResponse?.decks ?? []
  const [tab, setTab] = useState<Tab>('덱모음')
  const { data: locale } = useCDragonLocale()
  const patchVersion = metaDeckResponse?.patchVersion ?? '집계 대기'
  const dataRangeLabel = metaDeckResponse?.dataStartDate
    ? `${metaDeckResponse.dataStartDate} 이후 수집 데이터`
    : '집계 데이터 수집 전'

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>{patchVersion} 패치 기준 · {dataRangeLabel} · 선택률순 노출</p>
          </div>
          <div className={styles.rightControls}>
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
            <div className={styles.tabBar}>
              <button type="button" className={tab === '덱모음' ? styles.activeTab : ''} aria-pressed={tab === '덱모음'} onClick={() => setTab('덱모음')}>
                덱모음
              </button>
              <button type="button" className={tab === '메타통계' ? styles.activeTab : ''} aria-pressed={tab === '메타통계'} onClick={() => setTab('메타통계')}>
                메타통계
              </button>
            </div>
          </div>
        </div>

        {tab === '덱모음'
          ? <DeckListView decks={decks} locale={locale} />
          : <MetaStatsView decks={decks} locale={locale} />
        }
      </div>
    </AppLayout>
  )
}

export default Decks
