import { useCallback, useEffect, useRef, useState } from 'react'

type PageAction = number | ((currentPage: number) => number)

interface UseClampCurrentPageOptions {
  currentPage: number
  setCurrentPage: (page: PageAction) => void
  totalPages: number
}

function areResetKeysEqual(previousKeys: readonly unknown[], nextKeys: readonly unknown[]) {
  return previousKeys.length === nextKeys.length
    && previousKeys.every((previousKey, index) => Object.is(previousKey, nextKeys[index]))
}

function normalizePage(page: number) {
  return Number.isFinite(page) && page > 0 ? Math.floor(page) : 1
}

export function useResettablePage(resetKeys: readonly unknown[] = []) {
  const [storedPage, setStoredPage] = useState(1)
  const resetKeysRef = useRef(resetKeys)
  const shouldResetPage = !areResetKeysEqual(resetKeysRef.current, resetKeys)
  const currentPage = shouldResetPage ? 1 : storedPage

  const setCurrentPage = useCallback((nextPage: PageAction) => {
    setStoredPage((previousPage) => normalizePage(
      typeof nextPage === 'function' ? nextPage(previousPage) : nextPage,
    ))
  }, [])

  const resetPage = useCallback(() => {
    setStoredPage(1)
  }, [])

  useEffect(() => {
    if (!areResetKeysEqual(resetKeysRef.current, resetKeys)) {
      resetKeysRef.current = resetKeys
      setStoredPage(1)
    }
  }, [resetKeys])

  return {
    currentPage,
    resetPage,
    setCurrentPage,
  }
}

export function useClampCurrentPage({
  currentPage,
  setCurrentPage,
  totalPages,
}: UseClampCurrentPageOptions) {
  useEffect(() => {
    const maxPage = normalizePage(totalPages)

    if (currentPage > maxPage) {
      setCurrentPage(maxPage)
    }
  }, [currentPage, setCurrentPage, totalPages])
}
