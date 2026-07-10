import { useQuery } from '@tanstack/react-query'
import { getPatchNotes } from '../../../api/patchNotes'
import { LIVE_CONTENT_QUERY_OPTIONS } from '../../../hooks/liveContentQueryOptions'

const EMPTY_FALLBACK: never[] = []

function useLatestPatchNoteQuery() {
  return useQuery({
    queryFn: () => getPatchNotes(EMPTY_FALLBACK),
    // PatchNotes 페이지의 usePatchNotes()는 ['patch-notes', 'list']를 쓰며
    // 그 캐시에는 patchNotesFallbackData가 채워져야 한다. 같은 키를 쓰면 이
    // 훅이 먼저 실행돼 빈 fallback([])으로 캐시를 채워버려 PatchNotes 페이지의
    // fallback 계약이 깨질 수 있어 별도 키를 쓴다.
    queryKey: ['patch-notes', 'latest-version'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })
}

export function useLatestPatchVersion() {
  const { data } = useLatestPatchNoteQuery()
  return data?.data[0]?.version
}

export function useLatestPatchNote() {
  const { data } = useLatestPatchNoteQuery()
  return data?.data[0]
}
