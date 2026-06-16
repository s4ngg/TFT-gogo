import type { PartyPost, PartyStatus } from '../../../api/partyApi'
import type { PartyFilter } from '../../Party/partyFilters'

const DEFAULT_PARTY_PREVIEW_LIMIT = 4

export function selectPartyPreviewPosts(
  posts: readonly PartyPost[],
  selectedFilter: PartyFilter,
  limit = DEFAULT_PARTY_PREVIEW_LIMIT,
): PartyPost[] {
  const safeLimit = Math.max(0, limit)
  const filteredPosts = selectedFilter === '전체'
    ? posts
    : posts.filter((post) => post.mode === selectedFilter)

  return filteredPosts.slice(0, safeLimit)
}

export function getPartyPreviewStatusLabel(status: PartyStatus) {
  return status === '대기중' ? '마감' : status
}
