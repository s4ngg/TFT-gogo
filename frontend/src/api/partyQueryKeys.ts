import type { PartyPostsQueryParams } from './partyApi'

export const COMMUNITY_PARTY_POSTS_QUERY_KEY = ['community', 'parties'] as const

export function communityPartyPostsQueryKey(params: PartyPostsQueryParams = {}) {
  return params.mode
    ? [...COMMUNITY_PARTY_POSTS_QUERY_KEY, { mode: params.mode }] as const
    : COMMUNITY_PARTY_POSTS_QUERY_KEY
}

export function communityPartyPostsScopedQueryKey(
  params: PartyPostsQueryParams = {},
  authUserId: string | null,
) {
  return [
    ...communityPartyPostsQueryKey(params),
    { authScope: authUserId ?? 'anonymous' },
  ] as const
}
