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
  const selectedPatch = selectedPatchFromQuery ?? patchNotesFallbackData[0]
  const {
    activeCategory,
    activeChangeType,
    changeStats,
    changesPage,
    expandedChangeIds,
    highImpactOnly,
    patchChanges,
    patchChangesQuery,
    query,
    safePage,
    setActiveCategory,
    setActiveChangeType,
    setCurrentPage,
    setQuery,
    toggleExpandedChange,
    toggleHighImpactOnly,
  } = usePatchNotesPageState({
    fallbackData: patchNotesFallbackData,
    patchHistory,
    selectedPatch,
    selectedPatchVersion,
  })

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
              onHighImpactOnlyToggle={toggleHighImpactOnly}
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
