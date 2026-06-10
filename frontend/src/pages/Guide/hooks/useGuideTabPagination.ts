import { useEffect, useState } from 'react'

interface UseGuideTabPaginationOptions {
  resetKey: unknown
}

export function useGuideTabPagination({
  resetKey,
}: UseGuideTabPaginationOptions) {
  const [currentPage, setCurrentPage] = useState(1)

  useEffect(() => {
    setCurrentPage(1)
  }, [resetKey])

  return {
    currentPage,
    setCurrentPage,
  }
}

interface UseGuidePageBoundsOptions {
  currentPage: number
  setCurrentPage: (page: number) => void
  totalPages: number
}

export function useGuidePageBounds({
  currentPage,
  setCurrentPage,
  totalPages,
}: UseGuidePageBoundsOptions) {
  const safePage = Math.min(currentPage, totalPages)

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages)
  }, [currentPage, setCurrentPage, totalPages])

  return safePage
}
