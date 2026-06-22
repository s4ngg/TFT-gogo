import { AppLayout } from '../../components/layout'
import { usePatchNotes } from '../../hooks/usePatchNotes'
import { patchNotesFallbackData } from '../../mocks/patchNotesMock'
import PatchChangeFilters from './components/PatchChangeFilters'
import PatchChangeList from './components/PatchChangeList'
import PatchHero from './components/PatchHero'
import PatchPagination from './components/PatchPagination'
import PatchSideRail from './components/PatchSideRail'
import PatchStatusBanner from './components/PatchStatusBanner'
import PatchSummaryGrid from './components/PatchSummaryGrid'
import { usePatchChangesPage } from './hooks/usePatchChangesPage'
import { usePatchNotesPageState } from './hooks/usePatchNotesPageState'
import styles from './PatchNotes.module.css'

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
  const selectedPatch = selectedPatchFromQuery
  const {
    activeCategory,
    patchChangesParams,
    query,
    resetChangeListState,
    setActiveCategory,
    setCurrentPage,
    setQuery,
  } = usePatchNotesPageState({
    selectedPatchVersion,
  })
  const {
    changeStats,
    changesPage,
    patchChanges,
    patchChangesQuery,
    safePage,
  } = usePatchChangesPage({
    fallbackData: patchNotesFallbackData,
    onPageOutOfRange: setCurrentPage,
    params: patchChangesParams,
    patchHistory,
  })
  const isPatchChangesFallback = patchChangesQuery.data?.source === 'fallback'
  const showFallbackStatus = (isFallbackData || isPatchChangesFallback) && !isFetching && !patchChangesQuery.isFetching

  function handlePatchSelect(version: string) {
    if (version === selectedPatchVersion) return

    resetChangeListState()
    setSelectedPatchVersion(version)
  }

  if (!selectedPatch) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <PatchStatusBanner
            isFallbackData={false}
            isFetching
            onRetry={() => {
              void refetchPatchNotes()
            }}
          />
        </div>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <PatchHero selectedPatch={selectedPatch} />

        <PatchStatusBanner
          isFallbackData={showFallbackStatus}
          isFetching={isFetching}
          onRetry={() => {
            void refetchPatchNotes()
            void patchChangesQuery.refetch()
          }}
        />

        <PatchSummaryGrid
          newCount={changeStats.typeCounts.신규}
          totalCount={changeStats.totalChanges}
        />

        <div className={styles.contentGrid}>
          <PatchSideRail
            activeCategory={activeCategory}
            categoryCounts={changeStats.categoryCounts}
            onCategorySelect={setActiveCategory}
            onPatchSelect={handlePatchSelect}
            patchHistory={patchHistory}
            selectedPatch={selectedPatch}
            selectedPatchVersion={selectedPatchVersion}
          />

          <section className={styles.changePanel}>
            <div className={styles.panelHeader}>
              <div>
                <span className={styles.sectionLabel}>카테고리별 변경사항</span>
                <h2>{selectedPatch.date} 적용 변경</h2>
              </div>
            </div>

            <PatchChangeFilters
              activeCategory={activeCategory}
              onCategoryChange={setActiveCategory}
              onQueryChange={setQuery}
              query={query}
              stats={changeStats}
            />

            <PatchChangeList
              patchChanges={patchChanges}
            />

            <PatchPagination currentPage={safePage} totalPages={changesPage.totalPages} onPageChange={setCurrentPage} />
          </section>
        </div>
      </div>
    </AppLayout>
  )
}

export default PatchNotes
