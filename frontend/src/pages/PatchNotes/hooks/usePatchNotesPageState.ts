import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  PATCH_CATEGORIES,
  type PatchChangesQuery,
  type PatchCategory,
} from '../../../api/patchNotes'

export const PATCH_CHANGE_PAGE_SIZE = 1000
const SEARCH_QUERY_DEBOUNCE_MS = 300

interface UsePatchNotesPageStateOptions {
  selectedPatchVersion: string
}

export function usePatchNotesPageState({
  selectedPatchVersion,
}: UsePatchNotesPageStateOptions) {
  const [activeCategory, setActiveCategory] = useState<PatchCategory>(PATCH_CATEGORIES[0])
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
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
      pageSize: PATCH_CHANGE_PAGE_SIZE,
      query: debouncedQuery,
      version: selectedPatchVersion,
    }),
    [activeCategory, currentPage, debouncedQuery, selectedPatchVersion],
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
    const timeoutId = window.setTimeout(() => {
      setDebouncedQuery(query)
    }, SEARCH_QUERY_DEBOUNCE_MS)

    return () => window.clearTimeout(timeoutId)
  }, [query])

  useEffect(() => {
    if (selectedPatchVersionRef.current === selectedPatchVersion) return

    selectedPatchVersionRef.current = selectedPatchVersion
    setActiveCategory(PATCH_CATEGORIES[0])
    setQuery('')
    setDebouncedQuery('')
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
