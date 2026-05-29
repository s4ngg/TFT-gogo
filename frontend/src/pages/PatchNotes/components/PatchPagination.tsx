import styles from '../PatchNotes.module.css'

const PAGE_NUMBER_WINDOW = 5

interface PatchPaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

function getPaginationWindow(currentPage: number, totalPages: number) {
  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)

  return Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)
}

function PatchPagination({ currentPage, totalPages, onPageChange }: PatchPaginationProps) {
  if (totalPages <= 1) return null

  const pages = getPaginationWindow(currentPage, totalPages)
  const windowStart = pages[0]
  const windowEnd = pages[pages.length - 1]

  return (
    <nav className={styles.pagination} aria-label="패치 변경사항 페이지">
      {windowStart > 1 && (
        <button type="button" className={styles.pageMore} onClick={() => onPageChange(windowStart - 1)}>
          이전
        </button>
      )}

      {pages.map((page) => (
        <button
          key={page}
          type="button"
          className={currentPage === page ? styles.activePage : undefined}
          onClick={() => onPageChange(page)}
          aria-current={currentPage === page ? 'page' : undefined}
        >
          {page}
        </button>
      ))}

      {windowEnd < totalPages && (
        <button type="button" className={styles.pageMore} onClick={() => onPageChange(windowEnd + 1)}>
          더보기
        </button>
      )}
    </nav>
  )
}

export default PatchPagination
