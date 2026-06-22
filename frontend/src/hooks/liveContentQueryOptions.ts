export const LIVE_CONTENT_STALE_TIME_MS = 30_000
export const LIVE_CONTENT_REFETCH_INTERVAL_MS = 60_000

export const LIVE_CONTENT_QUERY_OPTIONS = {
  refetchInterval: LIVE_CONTENT_REFETCH_INTERVAL_MS,
  refetchOnReconnect: 'always',
  refetchOnWindowFocus: 'always',
  staleTime: LIVE_CONTENT_STALE_TIME_MS,
} as const
