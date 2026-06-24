import Pagination from '../../../components/common/Pagination'

interface PatchPaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

function PatchPagination({ currentPage, totalPages, onPageChange }: PatchPaginationProps) {
  return (
    <Pagination
      ariaLabel="패치 변경사항 페이지"
      currentPage={currentPage}
      nextWindowLabel="더보기"
      onPageChange={onPageChange}
      previousWindowLabel="이전"
      showAdjacentControls={false}
      totalPages={totalPages}
    />
  )
}

export default PatchPagination
