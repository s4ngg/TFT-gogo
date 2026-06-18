import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  PATCH_CATEGORIES,
  type PatchChangesQuery,
  type PatchCategory,
} from '../../../api/patchNotes'

const PATCH_PAGE_SIZE = 1000

interface UsePatchNotesPageStateOptions {
  selectedPatchVersion: string
}

export function usePatchNotesPageState({
  selectedPatchVersion,
}: UsePatchNotesPageStateOptions) {
  const [activeCategory, setActiveCategory] = useState<PatchCategory>(PATCH_CATEGORIES[0])
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const selectedPatchVersionRef = useRef(selectedPatchVersion)

  const resetChangeListState = useCallback(() => {
    setCurrentPage((page) => (page === 1 ? page : 1))
  }, [])

  const patchChangesParams = useMemo<PatchChangesQuery>(
    () => ({
      category: activeCategory,
      changeType: '전체',
      highImpactOnly: false,
      page: currentPage,
      pageSize: PATCH_PAGE_SIZE,
      query,
      version: selectedPatchVersion,
    }),
    [activeCategory, currentPage, query, selectedPatchVersion],
  )

  const setActiveCategoryAndReset = useCallback((category: PatchCategory) => {
    setActiveCategory((currentCategory) => {
      if (currentCategory === category) return currentCategory

      resetChangeListState()
      return category
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
    currentPage,
    patchChangesParams,
    query,
    resetChangeListState,
    setActiveCategory: setActiveCategoryAndReset,
    setCurrentPage,
    setQuery: setQueryAndReset,
  }
}
