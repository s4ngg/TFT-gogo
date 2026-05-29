import {
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  Search,
} from 'lucide-react'
import {
  PAGE_NUMBER_WINDOW,
  type ChampionRef,
  type ItemRef,
  type SortDir,
} from '../../../api/guide'
import styles from '../Guide.module.css'

export function EmptyState() {
  return (
    <div className={styles.emptyState}>
      <Search size={18} />
      <span>검색 결과가 없습니다.</span>
    </div>
  )
}

export function GuideStatusBanner({
  isFallbackData,
  isFetching,
  onRetry,
}: {
  isFallbackData: boolean
  isFetching: boolean
  onRetry: () => void
}) {
  if (!isFetching && !isFallbackData) return null

  return (
    <div
      aria-live="polite"
      className={`${styles.statusBanner} ${isFetching ? styles.statusLoading : styles.statusFallback}`}
    >
      <span className={styles.statusIcon}>
        {isFetching ? <Loader2 size={16} /> : <AlertTriangle size={16} />}
      </span>
      <div>
        <strong>{isFetching ? '가이드 데이터를 불러오는 중입니다.' : '샘플 데이터로 표시 중입니다.'}</strong>
        <p>
          {isFetching
            ? '최신 가이드 응답을 확인하는 동안 현재 데이터를 유지합니다.'
            : '가이드 API 응답을 가져오지 못해 준비된 샘플 데이터를 보여주고 있습니다.'}
        </p>
      </div>
      {!isFetching && (
        <button onClick={onRetry} type="button">
          <RefreshCw size={14} />
          다시 시도
        </button>
      )}
    </div>
  )
}

export function SortHeaderButton({
  active,
  direction,
  label,
  onClick,
}: {
  active: boolean
  direction: SortDir
  label: string
  onClick: () => void
}) {
  return (
    <button className={styles.sortButton} onClick={onClick} type="button">
      {label}
      <span>{active ? (direction === 'asc' ? '▲' : '▼') : '↕'}</span>
    </button>
  )
}

export function GuidePagination({
  currentPage,
  onPageChange,
  totalPages,
}: {
  currentPage: number
  onPageChange: (page: number) => void
  totalPages: number
}) {
  if (totalPages <= 1) return null

  const windowStart = Math.floor((currentPage - 1) / PAGE_NUMBER_WINDOW) * PAGE_NUMBER_WINDOW + 1
  const windowEnd = Math.min(totalPages, windowStart + PAGE_NUMBER_WINDOW - 1)
  const pages = Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)

  return (
    <nav className={styles.pagination} aria-label="가이드 페이지">
      <button
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        <ChevronLeft size={15} />
        이전
      </button>
      {windowStart > 1 && <span className={styles.pageEllipsis}>...</span>}
      {pages.map((page) => (
        <button
          aria-current={currentPage === page ? 'page' : undefined}
          className={currentPage === page ? styles.activePage : ''}
          key={page}
          onClick={() => onPageChange(page)}
          type="button"
        >
          {page}
        </button>
      ))}
      {windowEnd < totalPages && <span className={styles.pageEllipsis}>...</span>}
      <button
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        다음
        <ChevronRight size={15} />
      </button>
    </nav>
  )
}

export function StatBadge({ label, value }: { label: string; value: string }) {
  return (
    <span className={styles.statBadge}>
      <small>{label}</small>
      <strong>{value}</strong>
    </span>
  )
}

export function LinkedChampionMini({
  champion,
  onSelect,
}: {
  champion: ChampionRef
  onSelect: (championName: string) => void
}) {
  return (
    <button
      className={styles.championMini}
      onClick={() => onSelect(champion.name)}
      title={`${champion.name} 챔피언 보기`}
      type="button"
    >
      <img src={champion.imageUrl} alt={champion.name} />
      <span>{champion.name}</span>
      <b>{champion.cost}</b>
    </button>
  )
}

export function ItemIconStrip({
  items,
  onItemSelect,
}: {
  items: ItemRef[]
  onItemSelect?: (itemName: string) => void
}) {
  return (
    <span className={styles.itemIconStrip}>
      {items.map((itemRef) => (
        onItemSelect ? (
          <button
            className={styles.itemIconButton}
            key={itemRef.name}
            onClick={(event) => {
              event.stopPropagation()
              onItemSelect(itemRef.name)
            }}
            onKeyDown={(event) => {
              event.stopPropagation()
            }}
            title={`${itemRef.name} 아이템 보기`}
            type="button"
          >
            <img src={itemRef.imageUrl} alt={itemRef.name} />
          </button>
        ) : (
          <img src={itemRef.imageUrl} alt={itemRef.name} title={itemRef.name} key={itemRef.name} />
        )
      ))}
    </span>
  )
}
