import { useEffect, useId, useRef, useState } from 'react'
import {
  AlertTriangle,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Gem,
  Loader2,
  Package,
  RefreshCw,
  Rows3,
  Search,
  Shield,
  Sparkles,
  Star,
  Swords,
  Trophy,
  X,
} from 'lucide-react'
import {
  CHAMPION_PAGE_SIZE,
  DEFAULT_GUIDE_PAGE_SIZE,
  GUIDE_TABS,
  PAGE_NUMBER_WINDOW,
  TRAIT_PAGE_SIZE,
  type AugmentPlan,
  type AugmentPlanKey,
  type ChampionCostFilter,
  type ChampionGuide,
  type ChampionRef,
  type GuideCatalog,
  type GuideTab,
  type ItemRef,
  type MetricSortKey,
  type RecentGuide,
  type RewardRow,
  type SortDir,
} from '../../api/guide'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { AppLayout } from '../../components/layout'
import { useGuide, useGuideTabItems } from '../../hooks/useGuide'
import { guideFallbackData } from '../../mocks/guideResponseMock'
import styles from './Guide.module.css'

const GUIDE_TAB_ICONS: Record<GuideTab, typeof BookOpen> = {
  augments: Sparkles,
  champions: Swords,
  items: Package,
  traits: Shield,
}

function getNextGuideTabIndex(key: string, currentIndex: number): number | undefined {
  const lastIndex = GUIDE_TABS.length - 1

  if (key === 'ArrowLeft' || key === 'ArrowUp') {
    return currentIndex === 0 ? lastIndex : currentIndex - 1
  }

  if (key === 'ArrowRight' || key === 'ArrowDown') {
    return currentIndex === lastIndex ? 0 : currentIndex + 1
  }

  if (key === 'Home') return 0
  if (key === 'End') return lastIndex

  return undefined
}

function EmptyState() {
  return (
    <div className={styles.emptyState}>
      <Search size={18} />
      <span>검색 결과가 없습니다.</span>
    </div>
  )
}

function GuideStatusBanner({
  isFallbackData,
  isFetching,
  onRetry,
}: {
  isFallbackData: boolean
  isFetching: boolean
  onRetry: () => void
}) {
  if (!isFetching && !isFallbackData) return null

  return (
    <div
      aria-live="polite"
      className={`${styles.statusBanner} ${isFetching ? styles.statusLoading : styles.statusFallback}`}
    >
      <span className={styles.statusIcon}>
        {isFetching ? <Loader2 size={16} /> : <AlertTriangle size={16} />}
      </span>
      <div>
        <strong>{isFetching ? '가이드 데이터를 불러오는 중입니다.' : '샘플 데이터로 표시 중입니다.'}</strong>
        <p>
          {isFetching
            ? '최신 가이드 응답을 확인하는 동안 현재 데이터를 유지합니다.'
            : '가이드 API 응답을 가져오지 못해 준비된 샘플 데이터를 보여주고 있습니다.'}
        </p>
      </div>
      {!isFetching && (
        <button onClick={onRetry} type="button">
          <RefreshCw size={14} />
          다시 시도
        </button>
      )}
    </div>
  )
}

function SortHeaderButton({
  active,
  direction,
  label,
  onClick,
}: {
  active: boolean
  direction: SortDir
  label: string
  onClick: () => void
}) {
  return (
    <button className={styles.sortButton} onClick={onClick} type="button">
      {label}
      <span>{active ? (direction === 'asc' ? '▲' : '▼') : '↕'}</span>
    </button>
  )
}

function Pagination({
  currentPage,
  onPageChange,
  totalPages,
}: {
  currentPage: number
  onPageChange: (page: number) => void
  totalPages: number
}) {
  if (totalPages <= 1) return null

  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)
  const pages = Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)

  return (
    <nav className={styles.pagination} aria-label="가이드 페이지">
      <button
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        <ChevronLeft size={15} />
        이전
      </button>
      {windowStart > 1 && <span className={styles.pageEllipsis}>...</span>}
      {pages.map((page) => (
        <button
          aria-current={currentPage === page ? 'page' : undefined}
          className={currentPage === page ? styles.activePage : ''}
          key={page}
          onClick={() => onPageChange(page)}
          type="button"
        >
          {page}
        </button>
      ))}
      {windowEnd < totalPages && <span className={styles.pageEllipsis}>...</span>}
      <button
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        다음
        <ChevronRight size={15} />
      </button>
    </nav>
  )
}

function StatBadge({ label, value }: { label: string; value: string }) {
  return (
    <span className={styles.statBadge}>
      <small>{label}</small>
      <strong>{value}</strong>
    </span>
  )
}

function LinkedChampionMini({
  champion,
  onSelect,
}: {
  champion: ChampionRef
  onSelect: (championName: string) => void
}) {
  return (
    <button
      className={styles.championMini}
      onClick={() => onSelect(champion.name)}
      title={`${champion.name} 챔피언 보기`}
      type="button"
    >
      <img src={champion.imageUrl} alt={champion.name} />
      <span>{champion.name}</span>
      <b>{champion.cost}</b>
    </button>
  )
}

function ItemIconStrip({
  items,
  onItemSelect,
}: {
  items: ItemRef[]
  onItemSelect?: (itemName: string) => void
}) {
  return (
    <span className={styles.itemIconStrip}>
      {items.map((itemRef) => (
        onItemSelect ? (
          <button
            className={styles.itemIconButton}
            key={itemRef.name}
            onClick={(event) => {
              event.stopPropagation()
              onItemSelect(itemRef.name)
            }}
            onKeyDown={(event) => {
              event.stopPropagation()
            }}
            title={`${itemRef.name} 아이템 보기`}
            type="button"
          >
            <img src={itemRef.imageUrl} alt={itemRef.name} />
          </button>
        ) : (
          <img src={itemRef.imageUrl} alt={itemRef.name} title={itemRef.name} key={itemRef.name} />
        )
      ))}
    </span>
  )
}

function GuideQuickAccess({
  favoriteChampions,
  onJump,
  recentGuides,
}: {
  favoriteChampions: string[]
  onJump: (tab: GuideTab, query: string, label?: string) => void
  recentGuides: RecentGuide[]
}) {
  if (favoriteChampions.length === 0 && recentGuides.length === 0) return null

  return (
    <section className={styles.quickAccess} aria-label="빠른 이동">
      {favoriteChampions.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>즐겨찾기</strong>
          {favoriteChampions.slice(0, 6).map((name) => (
            <button key={name} onClick={() => onJump('champions', name, name)} type="button">
              {name}
            </button>
          ))}
        </div>
      )}
      {recentGuides.length > 0 && (
        <div className={styles.quickGroup}>
          <strong>최근 본 가이드</strong>
          {recentGuides.slice(0, 6).map((guide) => (
            <button
              key={`${guide.tab}-${guide.query}`}
              onClick={() => onJump(guide.tab, guide.query, guide.label)}
              type="button"
            >
              {guide.label}
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

function ChampionDetailDialog({
  champion,
  isFavorite,
  onClose,
  onFavoriteToggle,
  onItemSelect,
}: {
  champion: ChampionGuide
  isFavorite: boolean
  onClose: () => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
}) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const titleId = useId()
  const traitsId = useId()

  useEffect(() => {
    closeButtonRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div className={styles.dialogBackdrop} role="presentation" onClick={onClose}>
      <section
        aria-describedby={traitsId}
        aria-labelledby={titleId}
        aria-modal="true"
        className={styles.championDialog}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <button
          aria-label={`${champion.name} 상세 정보 닫기`}
          className={styles.dialogCloseButton}
          onClick={onClose}
          ref={closeButtonRef}
          type="button"
        >
          <X size={17} />
        </button>
        <div className={styles.dialogHero}>
          <img src={champion.imageUrl} alt={champion.name} />
          <div>
            <strong id={titleId}>{champion.name}</strong>
            <span>{champion.role}</span>
            <p id={traitsId}>{champion.traits.join(' / ')}</p>
          </div>
        </div>
        <div className={styles.dialogItems}>
          <b>3신기</b>
          <ItemIconStrip items={champion.bestItems} onItemSelect={onItemSelect} />
        </div>
        <dl className={styles.dialogStats}>
          <div><dt>체력</dt><dd>{champion.stats.hp}</dd></div>
          <div><dt>공격력</dt><dd>{champion.stats.ad}</dd></div>
          <div><dt>공속</dt><dd>{champion.stats.attackSpeed}</dd></div>
          <div><dt>마나</dt><dd>{champion.stats.mana}</dd></div>
          <div><dt>방어</dt><dd>{champion.stats.armor}</dd></div>
          <div><dt>마저</dt><dd>{champion.stats.mr}</dd></div>
        </dl>
        <p className={styles.dialogPosition}>권장 배치: {champion.position}</p>
        <button
          aria-pressed={isFavorite}
          className={`${styles.dialogFavoriteButton} ${isFavorite ? styles.favoriteActive : ''}`}
          onClick={() => onFavoriteToggle(champion.name)}
          type="button"
        >
          <Star size={15} />
          {isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
        </button>
      </section>
    </div>
  )
}

function TraitGuideView({
  fallbackData,
  onChampionSelect,
  query,
}: {
  fallbackData: GuideCatalog
  onChampionSelect: (championName: string) => void
  query: string
}) {
  const [currentPage, setCurrentPage] = useState(1)
  const traitsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: TRAIT_PAGE_SIZE,
      query,
      tab: 'traits',
    },
  })
  const pageData = traitsQuery.data.data
  const safePage = Math.min(currentPage, pageData.totalPages)
  const visibleTraits = pageData.items

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > pageData.totalPages) setCurrentPage(pageData.totalPages)
  }, [currentPage, pageData.totalPages])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={traitsQuery.data.source === 'fallback' && !traitsQuery.isFetching}
        isFetching={traitsQuery.isFetching}
        onRetry={() => {
          void traitsQuery.refetch()
        }}
      />
      <section className={styles.traitGrid}>
        {visibleTraits.length === 0 && <EmptyState />}
        {visibleTraits.map((traitGuide) => (
          <article className={styles.traitCard} key={traitGuide.name}>
            <div className={styles.traitTop}>
              <TraitHexBadge
                count={traitGuide.count}
                iconUrl={traitGuide.iconUrl}
                name={traitGuide.name}
                tone={traitGuide.tone}
              />
              <div className={styles.traitTitle}>
                <h2>{traitGuide.name}</h2>
                <span>{traitGuide.type}</span>
              </div>
              <div className={styles.levelTrack}>
                {traitGuide.levels.map((level) => (
                  <b className={level === String(traitGuide.count) ? styles.levelActive : ''} key={level}>
                    {level}
                  </b>
                ))}
              </div>
            </div>
            <p>{traitGuide.summary}</p>
            <div className={styles.championLine}>
              {traitGuide.champions.map((championRef) => (
                <LinkedChampionMini champion={championRef} key={championRef.name} onSelect={onChampionSelect} />
              ))}
            </div>
            <div className={styles.tipLine}>
              {traitGuide.tips.map((tip) => (
                <span key={tip}>{tip}</span>
              ))}
            </div>
          </article>
        ))}
      </section>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

function ItemStatsView({
  fallbackData,
  onChampionSelect,
  query,
}: {
  fallbackData: GuideCatalog
  onChampionSelect: (championName: string) => void
  query: string
}) {
  const [currentPage, setCurrentPage] = useState(1)
  const [sortDir, setSortDir] = useState<SortDir>('desc')
  const [sortKey, setSortKey] = useState<MetricSortKey>('winRate')
  const itemsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: DEFAULT_GUIDE_PAGE_SIZE,
      query,
      sortDir,
      sortKey,
      tab: 'items',
    },
  })
  const pageData = itemsQuery.data.data
  const safePage = Math.min(currentPage, pageData.totalPages)
  const visibleItems = pageData.items

  function handleSort(nextSortKey: MetricSortKey) {
    if (sortKey === nextSortKey) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(nextSortKey)
      setSortDir(nextSortKey === 'avgPlace' ? 'asc' : 'desc')
    }
    setCurrentPage(1)
  }

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > pageData.totalPages) setCurrentPage(pageData.totalPages)
  }, [currentPage, pageData.totalPages])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={itemsQuery.data.source === 'fallback' && !itemsQuery.isFetching}
        isFetching={itemsQuery.isFetching}
        onRetry={() => {
          void itemsQuery.refetch()
        }}
      />
      <div className={styles.tableWrap}>
        <table className={styles.itemTable}>
          <thead>
            <tr>
              <th className={styles.nameCol}>아이템</th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'winRate'}
                  direction={sortDir}
                  label="승률"
                  onClick={() => handleSort('winRate')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'top4'}
                  direction={sortDir}
                  label="TOP4"
                  onClick={() => handleSort('top4')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'avgPlace'}
                  direction={sortDir}
                  label="평균 등수"
                  onClick={() => handleSort('avgPlace')}
                />
              </th>
              <th>
                <SortHeaderButton
                  active={sortKey === 'pickRate'}
                  direction={sortDir}
                  label="픽률"
                  onClick={() => handleSort('pickRate')}
                />
              </th>
              <th className={styles.userCol}>추천 챔피언</th>
              <th className={styles.comboCol}>조합 추천</th>
            </tr>
          </thead>
          <tbody>
            {visibleItems.map((itemStat) => (
              <tr key={itemStat.name}>
                <td className={styles.itemNameCell}>
                  <img src={itemStat.imageUrl} alt={itemStat.name} />
                  <div>
                    <strong>{itemStat.name}</strong>
                    <span>{itemStat.category}</span>
                  </div>
                </td>
                <td className={styles.winRate}>{itemStat.winRate}</td>
                <td className={styles.top4}>{itemStat.top4}</td>
                <td className={styles.avgPlace}>#{itemStat.avgPlace}</td>
                <td className={styles.pickRate}>{itemStat.pickRate}</td>
                <td>
                  <div className={styles.avatarStack}>
                    {itemStat.bestUsers.map((championRef) => (
                      <button
                        className={styles.avatarButton}
                        key={championRef.name}
                        onClick={() => onChampionSelect(championRef.name)}
                        title={`${championRef.name} 챔피언 보기`}
                        type="button"
                      >
                        <img src={championRef.imageUrl} alt={championRef.name} />
                      </button>
                    ))}
                  </div>
                </td>
                <td>
                  {itemStat.combinations.map((combination) => (
                    <div className={styles.comboCell} key={combination.label}>
                      <ItemIconStrip items={combination.items} />
                      <div>
                        <strong>{combination.label}</strong>
                        <span>{combination.note}</span>
                      </div>
                    </div>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {visibleItems.length === 0 && <EmptyState />}
      </div>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />

      <section className={styles.metricCards}>
        <article>
          <Rows3 size={18} />
          <strong>매치 기반 집계</strong>
          <span>matchId별 최종 배치와 장착 아이템을 묶어 승률, TOP4, 평균 등수를 계산합니다.</span>
        </article>
        <article>
          <Gem size={18} />
          <strong>3신기 우선순위</strong>
          <span>완성 아이템 3개 조합을 캐리 챔피언별로 비교할 수 있게 확장합니다.</span>
        </article>
        <article>
          <Trophy size={18} />
          <strong>표본 필터</strong>
          <span>마스터+와 전체 랭크를 분리해 메타 왜곡을 줄이는 구성이 좋습니다.</span>
        </article>
      </section>
    </>
  )
}

function AugmentGuideView({
  augmentPlans,
  fallbackData,
  query,
  rewardRows,
}: {
  augmentPlans: AugmentPlan[]
  fallbackData: GuideCatalog
  query: string
  rewardRows: RewardRow[]
}) {
  const [planKey, setPlanKey] = useState<AugmentPlanKey>('fast8')
  const [currentPage, setCurrentPage] = useState(1)
  const [sortDir, setSortDir] = useState<SortDir>('desc')
  const [sortKey, setSortKey] = useState<MetricSortKey>('winRate')
  const augmentsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: DEFAULT_GUIDE_PAGE_SIZE,
      query,
      sortDir,
      sortKey,
      tab: 'augments',
    },
  })
  const pageData = augmentsQuery.data.data
  const safePage = Math.min(currentPage, pageData.totalPages)
  const visibleAugments = pageData.items
  const selectedPlan = augmentPlans.find((plan) => plan.key === planKey) ?? augmentPlans[0]

  function handleSort(nextSortKey: MetricSortKey) {
    if (sortKey === nextSortKey) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(nextSortKey)
      setSortDir(nextSortKey === 'avgPlace' ? 'asc' : 'desc')
    }
    setCurrentPage(1)
  }

  useEffect(() => {
    setCurrentPage(1)
  }, [query])

  useEffect(() => {
    if (currentPage > pageData.totalPages) setCurrentPage(pageData.totalPages)
  }, [currentPage, pageData.totalPages])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={augmentsQuery.data.source === 'fallback' && !augmentsQuery.isFetching}
        isFetching={augmentsQuery.isFetching}
        onRetry={() => {
          void augmentsQuery.refetch()
        }}
      />
      <div className={styles.augmentLayout}>
        <section className={styles.tableWrap}>
          <table className={styles.augmentTable}>
            <thead>
              <tr>
                <th>티어</th>
                <th className={styles.nameCol}>증강체</th>
                <th>종류</th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'winRate'}
                    direction={sortDir}
                    label="승률"
                    onClick={() => handleSort('winRate')}
                  />
                </th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'avgPlace'}
                    direction={sortDir}
                    label="평균 등수"
                    onClick={() => handleSort('avgPlace')}
                  />
                </th>
                <th>
                  <SortHeaderButton
                    active={sortKey === 'pickRate'}
                    direction={sortDir}
                    label="픽률"
                    onClick={() => handleSort('pickRate')}
                  />
                </th>
                <th className={styles.rewardCol}>보상</th>
              </tr>
            </thead>
            <tbody>
              {visibleAugments.map((augment) => (
                <tr key={augment.name}>
                  <td><TierBadge value={augment.tier} /></td>
                  <td className={styles.augmentNameCell}>
                    <strong>{augment.name}</strong>
                    <span>{augment.description}</span>
                    <div>
                      {augment.tags.map((tag) => <b key={tag}>{tag}</b>)}
                    </div>
                  </td>
                  <td>{augment.type}</td>
                  <td className={styles.winRate}>{augment.winRate}</td>
                  <td className={styles.avgPlace}>#{augment.avgPlace}</td>
                  <td className={styles.pickRate}>{augment.pickRate}</td>
                  <td className={styles.rewardCell}>{augment.reward}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {visibleAugments.length === 0 && <EmptyState />}
        </section>

        <aside className={styles.rewardPanel}>
          <div className={styles.panelHeading}>
            <Trophy size={17} />
            <h2>보상표</h2>
          </div>
          <div className={styles.rewardList}>
            {rewardRows.map((row) => (
              <div className={styles.rewardRow} key={`${row.stage}-${row.condition}`}>
                <b>{row.stage}</b>
                <strong>{row.condition}</strong>
                <span>{row.reward}</span>
              </div>
            ))}
          </div>
        </aside>
      </div>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />

      <section className={styles.plannerPanel}>
        <div className={styles.plannerTop}>
          <div>
            <span className={styles.sectionBadge}>배치툴</span>
            <h2>증강 선택 플랜</h2>
          </div>
          <div className={styles.planTabs}>
            {augmentPlans.map((plan) => (
              <button
                className={plan.key === planKey ? styles.planActive : ''}
                key={plan.key}
                onClick={() => setPlanKey(plan.key)}
                type="button"
              >
                {plan.label}
              </button>
            ))}
          </div>
        </div>

        {selectedPlan && (
          <div className={styles.plannerBody}>
            <div className={styles.stageCards}>
              {selectedPlan.stages.map((stage) => (
                <article className={styles.stageCard} key={`${selectedPlan.key}-${stage.stage}`}>
                  <span>{stage.stage}</span>
                  <strong>{stage.choice}</strong>
                  <p>{stage.focus}</p>
                </article>
              ))}
            </div>
            <div className={styles.boardTool} aria-label="증강 선택 이후 배치 미리보기">
              {Array.from({ length: 21 }).map((_, index) => (
                <span
                  className={
                    index === 2 || index === 4 || index === 10 || index === 16
                      ? styles.boardCellActive
                      : ''
                  }
                  key={index}
                />
              ))}
            </div>
          </div>
        )}
      </section>
    </>
  )
}

function ChampionGuideView({
  fallbackData,
  favoriteChampions,
  onChampionOpen,
  onFavoriteToggle,
  onItemSelect,
  query,
}: {
  fallbackData: GuideCatalog
  favoriteChampions: string[]
  onChampionOpen: (championName: string) => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
  query: string
}) {
  const [costFilter, setCostFilter] = useState<ChampionCostFilter>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const [selectedChampion, setSelectedChampion] = useState<ChampionGuide | null>(null)
  const lastFocusedElementRef = useRef<HTMLElement | null>(null)
  const championsQuery = useGuideTabItems({
    fallbackData,
    params: {
      cost: costFilter,
      page: currentPage,
      pageSize: CHAMPION_PAGE_SIZE,
      query,
      tab: 'champions',
    },
  })
  const pageData = championsQuery.data.data
  const safePage = Math.min(currentPage, pageData.totalPages)
  const visibleChampions = pageData.items

  useEffect(() => {
    setCurrentPage(1)
  }, [costFilter, query])

  useEffect(() => {
    if (currentPage > pageData.totalPages) setCurrentPage(pageData.totalPages)
  }, [currentPage, pageData.totalPages])

  function openChampionDetail(championGuide: ChampionGuide) {
    lastFocusedElementRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setSelectedChampion(championGuide)
    onChampionOpen(championGuide.name)
  }

  function closeChampionDetail() {
    setSelectedChampion(null)
    window.requestAnimationFrame(() => {
      if (lastFocusedElementRef.current?.isConnected) {
        lastFocusedElementRef.current.focus()
      }
    })
  }

  return (
    <>
      <GuideStatusBanner
        isFallbackData={championsQuery.data.source === 'fallback' && !championsQuery.isFetching}
        isFetching={championsQuery.isFetching}
        onRetry={() => {
          void championsQuery.refetch()
        }}
      />
      <div className={styles.costFilter} aria-label="챔피언 비용 필터">
        {(['all', 1, 2, 3, 4, 5] as const).map((cost) => (
          <button
            className={costFilter === cost ? styles.costActive : ''}
            key={cost}
            onClick={() => setCostFilter(cost)}
            aria-pressed={costFilter === cost}
            type="button"
          >
            {cost === 'all' ? '전체' : `${cost}코스트`}
          </button>
        ))}
      </div>
      <section className={styles.championGrid}>
        {visibleChampions.length === 0 && <EmptyState />}
        {visibleChampions.map((championGuide) => (
          <article
            className={styles.championCard}
            key={championGuide.name}
            onClick={() => {
              openChampionDetail(championGuide)
            }}
            onKeyDown={(event) => {
              if (event.target !== event.currentTarget) return
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                openChampionDetail(championGuide)
              }
            }}
            role="button"
            tabIndex={0}
          >
            <button
              aria-pressed={favoriteChampions.includes(championGuide.name)}
              className={`${styles.favoriteButton} ${favoriteChampions.includes(championGuide.name) ? styles.favoriteActive : ''}`}
              onClick={(event) => {
                event.stopPropagation()
                onFavoriteToggle(championGuide.name)
              }}
              onKeyDown={(event) => {
                event.stopPropagation()
              }}
              title={favoriteChampions.includes(championGuide.name) ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              type="button"
            >
              <Star size={14} />
            </button>
            <div className={styles.championPortrait}>
              <img src={championGuide.imageUrl} alt={championGuide.name} />
              <span>{championGuide.cost}</span>
            </div>
            <div className={styles.championInfo}>
              <strong>{championGuide.name}</strong>
              <span>{championGuide.role}</span>
            </div>
            <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
            <div className={styles.championTooltip} role="tooltip">
              <div className={styles.tooltipTop}>
                <img src={championGuide.imageUrl} alt="" />
                <div>
                  <strong>{championGuide.name}</strong>
                  <span>{championGuide.traits.join(' / ')}</span>
                </div>
              </div>
              <div className={styles.tooltipItems}>
                <b>3신기</b>
                <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
              </div>
              <dl className={styles.statGrid}>
                <div><dt>체력</dt><dd>{championGuide.stats.hp}</dd></div>
                <div><dt>공격력</dt><dd>{championGuide.stats.ad}</dd></div>
                <div><dt>공속</dt><dd>{championGuide.stats.attackSpeed}</dd></div>
                <div><dt>마나</dt><dd>{championGuide.stats.mana}</dd></div>
                <div><dt>방어</dt><dd>{championGuide.stats.armor}</dd></div>
                <div><dt>마저</dt><dd>{championGuide.stats.mr}</dd></div>
              </dl>
              <p>권장 배치: {championGuide.position}</p>
            </div>
          </article>
        ))}
      </section>
      <Pagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
      {selectedChampion && (
        <ChampionDetailDialog
          champion={selectedChampion}
          isFavorite={favoriteChampions.includes(selectedChampion.name)}
          onClose={closeChampionDetail}
          onFavoriteToggle={onFavoriteToggle}
          onItemSelect={(itemName) => {
            closeChampionDetail()
            onItemSelect(itemName)
          }}
        />
      )}
    </>
  )
}

function Guide() {
  const {
    activeTab,
    activeTabInfo,
    addRecentGuide,
    favoriteChampions,
    guideData,
    handleFavoriteToggle,
    jumpToGuide,
    recentGuides,
    search,
    selectTab,
    setSearch,
  } = useGuide({ fallbackData: guideFallbackData })

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <span className={styles.kicker}>
              <BookOpen size={15} />
              SET 17 GUIDE
            </span>
            <h1>게임 가이드</h1>
            <p>시너지, 아이템, 증강체, 챔피언 정보를 한 화면에서 빠르게 비교합니다.</p>
          </div>
          <div className={styles.headerStats}>
            <StatBadge label="기준 패치" value={guideData.patchVersion} />
          </div>
        </header>

        <section className={styles.controlPanel}>
          <div className={styles.tabBar} role="tablist" aria-label="게임 가이드 탭">
            {GUIDE_TABS.map(({ key, label, meta }, guideTabIndex) => {
              const Icon = GUIDE_TAB_ICONS[key]

              return (
                <button
                  aria-controls={`guide-panel-${key}`}
                  aria-selected={activeTab === key}
                  className={activeTab === key ? styles.activeTab : ''}
                  id={`guide-tab-${key}`}
                  key={key}
                  onClick={() => selectTab(key)}
                  onKeyDown={(event) => {
                    const nextTabIndex = getNextGuideTabIndex(event.key, guideTabIndex)
                    if (nextTabIndex === undefined) return

                    event.preventDefault()
                    const nextTab = GUIDE_TABS[nextTabIndex]
                    selectTab(nextTab.key)
                    window.requestAnimationFrame(() => {
                      document.getElementById(`guide-tab-${nextTab.key}`)?.focus()
                    })
                  }}
                  role="tab"
                  tabIndex={activeTab === key ? 0 : -1}
                  type="button"
                >
                  <Icon size={18} />
                  <span>{label}</span>
                  <small>{meta}</small>
                </button>
              )
            })}
          </div>
          <label className={styles.searchBox}>
            <Search size={15} />
            <input
              onChange={(event) => setSearch(event.target.value)}
              placeholder={`${activeTabInfo.label} 검색`}
              value={search}
            />
          </label>
        </section>

        <GuideQuickAccess
          favoriteChampions={favoriteChampions}
          onJump={jumpToGuide}
          recentGuides={recentGuides}
        />

        {activeTab === 'traits' && (
          <div id="guide-panel-traits" role="tabpanel" aria-labelledby="guide-tab-traits">
            <TraitGuideView
              fallbackData={guideData}
              onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
              query={search}
            />
          </div>
        )}
        {activeTab === 'items' && (
          <div id="guide-panel-items" role="tabpanel" aria-labelledby="guide-tab-items">
            <ItemStatsView
              fallbackData={guideData}
              onChampionSelect={(championName) => jumpToGuide('champions', championName, championName)}
              query={search}
            />
          </div>
        )}
        {activeTab === 'augments' && (
          <div id="guide-panel-augments" role="tabpanel" aria-labelledby="guide-tab-augments">
            <AugmentGuideView
              augmentPlans={guideData.augmentPlans}
              fallbackData={guideData}
              query={search}
              rewardRows={guideData.rewards}
            />
          </div>
        )}
        {activeTab === 'champions' && (
          <div id="guide-panel-champions" role="tabpanel" aria-labelledby="guide-tab-champions">
            <ChampionGuideView
              fallbackData={guideData}
              favoriteChampions={favoriteChampions}
              onChampionOpen={(championName) => addRecentGuide({ label: championName, query: championName, tab: 'champions' })}
              onFavoriteToggle={handleFavoriteToggle}
              onItemSelect={(itemName) => jumpToGuide('items', itemName, itemName)}
              query={search}
            />
          </div>
        )}
      </div>
    </AppLayout>
  )
}

export default Guide
