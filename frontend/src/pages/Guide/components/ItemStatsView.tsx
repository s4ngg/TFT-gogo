import { useEffect, useState } from 'react'
import { Gem, Rows3, Trophy } from 'lucide-react'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  type GuideCatalog,
  type MetricSortKey,
  type SortDir,
} from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
  ItemIconStrip,
  SortHeaderButton,
} from './GuideShared'
import styles from '../Guide.module.css'

interface ItemStatsViewProps {
  fallbackData: GuideCatalog
  onChampionSelect: (championName: string) => void
  query: string
}

function ItemStatsView({
  fallbackData,
  onChampionSelect,
  query,
}: ItemStatsViewProps) {
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
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />

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

export default ItemStatsView
