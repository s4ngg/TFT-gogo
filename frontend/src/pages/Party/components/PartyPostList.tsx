import Pagination from '../../../components/common/Pagination'
import type { PartyPost } from '../types'
import PartyPostCard from './PartyPostCard'
import styles from '../Party.module.css'

interface PartyPostListProps {
  currentPage: number
  emptyMessage: string
  isAuthenticated: boolean
  isLoading: boolean
  isUnavailable: boolean
  joinedPostId: string | null
  joiningPostId: string | null
  onJoinToggle: (postId: string) => void
  onPageChange: (page: number) => void
  posts: PartyPost[]
  totalPages: number
}

function PartyPostList({
  currentPage,
  emptyMessage,
  isAuthenticated,
  isLoading,
  isUnavailable,
  joinedPostId,
  joiningPostId,
  onJoinToggle,
  onPageChange,
  posts,
  totalPages,
}: PartyPostListProps) {
  return (
    <>
      <div aria-busy={isLoading} className={styles.partyList}>
        {isLoading || isUnavailable ? (
          <p className={styles.emptyState}>{emptyMessage}</p>
        ) : posts.length > 0 ? (
          posts.map((post) => {
            const isJoined = joinedPostId === post.id || post.isJoined === true
            const isOwner = post.isOwner === true
            const hasJoinedOtherPost = joinedPostId !== null && !isJoined

            return (
              <PartyPostCard
                hasJoinedOtherPost={hasJoinedOtherPost}
                isAuthenticated={isAuthenticated}
                isJoined={isJoined}
                isJoinPending={joiningPostId === post.id}
                isOwner={isOwner}
                key={post.id}
                onJoinToggle={onJoinToggle}
                post={post}
              />
            )
          })
        ) : (
          <p className={styles.emptyState}>{emptyMessage}</p>
        )}
      </div>

      <div className={styles.partyPagination}>
        <Pagination
          ariaLabel="파티 모집글 페이지"
          currentPage={currentPage}
          onPageChange={onPageChange}
          totalPages={totalPages}
        />
      </div>
    </>
  )
}

export default PartyPostList
