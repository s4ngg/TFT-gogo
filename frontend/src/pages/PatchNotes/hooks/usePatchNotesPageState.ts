import { useEffect, useState } from 'react'
import {
  CHANGE_TYPE_FILTERS,
  PATCH_CATEGORIES,
  type PatchNoteDetail,
  type ChangeTypeFilter,
  type PatchCategory,
} from '../../../api/patchNotes'
import { usePatchChanges } from '../../../hooks/usePatchNotes'

const PATCH_PAGE_SIZE = 5

interface UsePatchNotesPageStateOptions {
  fallbackData: PatchNoteDetail[]
  patchHistory: PatchNoteDetail[]
  selectedPatchVersion: string
}

export function usePatchNotesPageState({
  fallbackData,
  patchHistory,
  selectedPatchVersion,
}: UsePatchNotesPageStateOptions) {
  const [activeCategory, setActiveCategory] = useState<PatchCategory>(PATCH_CATEGORIES[0])
  const [activeChangeType, setActiveChangeType] = useState<ChangeTypeFilter>(CHANGE_TYPE_FILTERS[0])
  const [highImpactOnly, setHighImpactOnly] = useState(false)
  const [expandedChangeIds, setExpandedChangeIds] = useState<number[]>([])
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const patchChangesQuery = usePatchChanges({
    fallbackData: patchHistory.length > 0 ? patchHistory : fallbackData,
    params: {
      category: activeCategory,
      changeType: activeChangeType,
      highImpactOnly,
      page: currentPage,
      pageSize: PATCH_PAGE_SIZE,
      query,
      version: selectedPatchVersion,
    },
  })
  const changesPage = patchChangesQuery.data.data
  const patchChanges = changesPage.items
  const changeStats = changesPage.stats
  const safePage = Math.max(1, Math.min(currentPage, changesPage.totalPages))

  function toggleHighImpactOnly() {
    setHighImpactOnly((enabled) => !enabled)
  }

  function toggleExpandedChange(id: number) {
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
    if (changesPage.totalPages < 1) {
      if (currentPage !== 1) setCurrentPage(1)
      return
    }

    if (currentPage > changesPage.totalPages) setCurrentPage(changesPage.totalPages)
  }, [changesPage.totalPages, currentPage])

  return {
    activeCategory,
    activeChangeType,
    changeStats,
    changesPage,
    currentPage,
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
  }
}
