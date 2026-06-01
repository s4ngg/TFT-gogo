import { useEffect, useState } from 'react'
import { Trophy } from 'lucide-react'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  type AugmentPlan,
  type AugmentPlanKey,
  type GuideCatalog,
  type MetricSortKey,
  type RewardRow,
  type SortDir,
} from '../../../api/guide'
import TierBadge from '../../../components/common/TierBadge'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
  SortHeaderButton,
} from './GuideShared'
import styles from '../Guide.module.css'

interface AugmentGuideViewProps {
  augmentPlans: AugmentPlan[]
  fallbackData: GuideCatalog
  query: string
  rewardRows: RewardRow[]
}

function AugmentGuideView({
  augmentPlans,
  fallbackData,
  query,
  rewardRows,
}: AugmentGuideViewProps) {
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
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />

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

export default AugmentGuideView
