import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  CHANGE_TYPE_FILTERS,
  PATCH_CATEGORIES,
  type ChangeTypeFilter,
  type PatchChangesQuery,
  type PatchCategory,
} from '../../../api/patchNotes'

const PATCH_PAGE_SIZE = 50

interface UsePatchNotesPageStateOptions {
  selectedPatchVersion: string
}

export function usePatchNotesPageState({
  selectedPatchVersion,
}: UsePatchNotesPageStateOptions) {
  const [activeCategory, setActiveCategory] = useState<PatchCategory>(PATCH_CATEGORIES[0])
  const [activeChangeType, setActiveChangeType] = useState<ChangeTypeFilter>(CHANGE_TYPE_FILTERS[0])
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const selectedPatchVersionRef = useRef(selectedPatchVersion)

  const resetChangeListState = useCallback(() => {
    setCurrentPage((page) => (page === 1 ? page : 1))
  }, [])

  const patchChangesParams = useMemo<PatchChangesQuery>(
    () => ({
      category: activeCategory,
      changeType: activeChangeType,
      highImpactOnly: false,
      page: currentPage,
      pageSize: PATCH_PAGE_SIZE,
      query,
      version: selectedPatchVersion,
    }),
    [activeCategory, activeChangeType, currentPage, query, selectedPatchVersion],
  )

  const setActiveCategoryAndReset = useCallback((category: PatchCategory) => {
    setActiveCategory((currentCategory) => {
      if (currentCategory === category) return currentCategory

      resetChangeListState()
      return category
    })
  }, [resetChangeListState])

  const setActiveChangeTypeAndReset = useCallback((changeType: ChangeTypeFilter) => {
    setActiveChangeType((currentChangeType) => {
      if (currentChangeType === changeType) return currentChangeType

      resetChangeListState()
      return changeType
    })
  }, [resetChangeListState])

  const setQueryAndReset = useCallback((nextQuery: string) => {
    setQuery((currentQuery) => {
      if (currentQuery === nextQuery) return currentQuery

      resetChangeListState()
      return nextQuery
    })
  }, [resetChangeListState])

  useEffect(() => {
    if (selectedPatchVersionRef.current === selectedPatchVersion) return

    selectedPatchVersionRef.current = selectedPatchVersion
    resetChangeListState()
  }, [resetChangeListState, selectedPatchVersion])

  return {
    activeCategory,
    activeChangeType,
    currentPage,
    patchChangesParams,
    query,
    resetChangeListState,
    setActiveCategory: setActiveCategoryAndReset,
    setActiveChangeType: setActiveChangeTypeAndReset,
    setCurrentPage,
    setQuery: setQueryAndReset,
  }
}
