import { useEffect, useState } from 'react'
import type { ChangeTypeFilter, PatchCategory } from '../../api/patchNotes'
import { AppLayout } from '../../components/layout'
import { usePatchChanges, usePatchNotes } from '../../hooks/usePatchNotes'
import { patchNotesFallbackData } from '../../mocks/patchNotesMock'
import PatchChangeFilters from './components/PatchChangeFilters'
import PatchChangeList from './components/PatchChangeList'
import PatchHero from './components/PatchHero'
import PatchPagination from './components/PatchPagination'
import PatchSideRail from './components/PatchSideRail'
import PatchStatusBanner from './components/PatchStatusBanner'
import PatchSummaryGrid from './components/PatchSummaryGrid'
import styles from './PatchNotes.module.css'

const PATCH_PAGE_SIZE = 5

function PatchNotes() {
  const {
    isFallbackData,
    isFetching,
    patchNotes: patchHistory,
    refetchPatchNotes,
    selectedPatch: selectedPatchFromQuery,
    selectedPatchVersion,
    setSelectedPatchVersion,
  } = usePatchNotes({ fallbackData: patchNotesFallbackData })
  const [activeCategory, setActiveCategory] = useState<PatchCategory>('전체')
  const [activeChangeType, setActiveChangeType] = useState<ChangeTypeFilter>('전체 변경')
  const [highImpactOnly, setHighImpactOnly] = useState(false)
  const [expandedChangeIds, setExpandedChangeIds] = useState<number[]>([])
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const selectedPatch = selectedPatchFromQuery ?? patchNotesFallbackData[0]
  const patchChangesQuery = usePatchChanges({
    fallbackData: patchHistory.length > 0 ? patchHistory : patchNotesFallbackData,
    params: {
      category: activeCategory,
      changeType: activeChangeType,
      highImpactOnly,
      page: currentPage,
      pageSize: PATCH_PAGE_SIZE,
      query,
      version: selectedPatch.version,
    },
  })
  const changesPage = patchChangesQuery.data.data
  const patchChanges = changesPage.items
  const changeStats = changesPage.stats
  const safePage = Math.min(currentPage, changesPage.totalPages)

  const toggleExpandedChange = (id: number) => {
    setExpandedChangeIds((currentIds) => (
      currentIds.includes(id)
        ? currentIds.filter((currentId) => currentId !== id)
        : [...currentIds, id]
    ))
  }

  useEffect(() => {
    setCurrentPage(1)
    setExpandedChangeIds([])
  }, [activeCategory, activeChangeType, highImpactOnly, query, selectedPatchVersion])

  useEffect(() => {
    if (currentPage > changesPage.totalPages) setCurrentPage(changesPage.totalPages)
  }, [changesPage.totalPages, currentPage])

  return (
    <AppLayout>
      <div className={styles.page}>
        <PatchHero changeStats={changeStats} selectedPatch={selectedPatch} />

        <PatchStatusBanner
          isFallbackData={(isFallbackData || patchChangesQuery.data.source === 'fallback') && !isFetching && !patchChangesQuery.isFetching}
          isFetching={isFetching || patchChangesQuery.isFetching}
          onRetry={() => {
            void refetchPatchNotes()
            void patchChangesQuery.refetch()
          }}
        />

        <PatchSummaryGrid buffCount={changeStats.buffCount} nerfCount={changeStats.nerfCount} />

        <div className={styles.contentGrid}>
          <section className={styles.changePanel}>
            <div className={styles.panelHeader}>
              <div>
                <span className={styles.sectionLabel}>변경사항</span>
                <h2>패치 상세 목록</h2>
              </div>
            </div>

            <PatchChangeFilters
              activeCategory={activeCategory}
              activeChangeType={activeChangeType}
              highImpactOnly={highImpactOnly}
              onCategoryChange={setActiveCategory}
              onChangeTypeChange={setActiveChangeType}
              onHighImpactOnlyToggle={() => setHighImpactOnly((enabled) => !enabled)}
              onQueryChange={setQuery}
              query={query}
              stats={changeStats}
            />

            <PatchChangeList
              expandedChangeIds={expandedChangeIds}
              onToggleExpandedChange={toggleExpandedChange}
              patchChanges={patchChanges}
            />

            <PatchPagination currentPage={safePage} totalPages={changesPage.totalPages} onPageChange={setCurrentPage} />
          </section>

          <PatchSideRail
            onPatchSelect={setSelectedPatchVersion}
            patchHistory={patchHistory}
            selectedPatch={selectedPatch}
            selectedPatchVersion={selectedPatchVersion}
          />
        </div>
      </div>
    </AppLayout>
  )
}

export default PatchNotes
