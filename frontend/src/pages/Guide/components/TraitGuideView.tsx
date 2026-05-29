import { useEffect, useState } from 'react'
import {
  TRAIT_PAGE_SIZE,
  type GuideCatalog,
} from '../../../api/guide'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
  LinkedChampionMini,
} from './GuideShared'
import styles from '../Guide.module.css'

interface TraitGuideViewProps {
  fallbackData: GuideCatalog
  onChampionSelect: (championName: string) => void
  query: string
}

function TraitGuideView({
  fallbackData,
  onChampionSelect,
  query,
}: TraitGuideViewProps) {
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
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default TraitGuideView
