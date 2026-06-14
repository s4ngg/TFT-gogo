import { ChevronLeft, ChevronRight } from 'lucide-react'
import styles from './Pagination.module.css'

const PAGE_NUMBER_WINDOW = 5

interface PaginationProps {
  ariaLabel: string
  currentPage: number
  onPageChange: (page: number) => void
  nextWindowLabel?: string
  previousWindowLabel?: string
  showAdjacentControls?: boolean
  totalPages: number
}

function getPaginationWindow(currentPage: number, totalPages: number) {
  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)

  return Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)
}

function Pagination({
  ariaLabel,
  currentPage,
  nextWindowLabel = `다음 ${PAGE_NUMBER_WINDOW}`,
  onPageChange,
  previousWindowLabel = `이전 ${PAGE_NUMBER_WINDOW}`,
  showAdjacentControls = true,
  totalPages,
}: PaginationProps) {
  if (totalPages <= 1) return null

  const pages = getPaginationWindow(currentPage, totalPages)
  const windowStart = pages[0]
  const windowEnd = pages[pages.length - 1]

  return (
    <nav className={styles.pagination} aria-label={ariaLabel}>
      {showAdjacentControls && (
        <button
          disabled={currentPage === 1}
          onClick={() => onPageChange(currentPage - 1)}
          type="button"
        >
          <ChevronLeft size={15} />
          이전
        </button>
      )}

      {windowStart > 1 && (
        <button className={styles.pageMore} onClick={() => onPageChange(windowStart - 1)} type="button">
          {previousWindowLabel}
        </button>
      )}

      {pages.map((page) => (
        <button
          aria-current={currentPage === page ? 'page' : undefined}
          className={currentPage === page ? styles.activePage : undefined}
          key={page}
          onClick={() => onPageChange(page)}
          type="button"
        >
          {page}
        </button>
      ))}

      {windowEnd < totalPages && (
        <button className={styles.pageMore} onClick={() => onPageChange(windowEnd + 1)} type="button">
          {nextWindowLabel}
        </button>
      )}

      {showAdjacentControls && (
        <button
          disabled={currentPage === totalPages}
          onClick={() => onPageChange(currentPage + 1)}
          type="button"
        >
          다음
          <ChevronRight size={15} />
        </button>
      )}
    </nav>
  )
}

export default Pagination
