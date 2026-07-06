import { useQuery } from '@tanstack/react-query'
import { getPatchNotes } from '../../../api/patchNotes'
import { LIVE_CONTENT_QUERY_OPTIONS } from '../../../hooks/liveContentQueryOptions'

const EMPTY_FALLBACK: never[] = []

export function useLatestPatchVersion() {
  const { data } = useQuery({
    queryFn: () => getPatchNotes(EMPTY_FALLBACK),
    queryKey: ['patch-notes', 'list'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })

  return data?.data[0]?.version
}
