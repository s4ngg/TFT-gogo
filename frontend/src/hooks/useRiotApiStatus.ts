import { useQuery } from '@tanstack/react-query'
import { getRiotApiStatus, type RiotApiStatusKind } from '../api/riotApi'

export interface RiotApiStatus {
  activeConnections: number
  checkedAt: string
  message: string
  queueSize: number
  source: 'api' | 'fallback'
  status: RiotApiStatusKind
}

export const fallbackRiotApiStatus: RiotApiStatus = {
  activeConnections: 0,
  checkedAt: new Date(0).toISOString(),
  message: '상태 API 응답을 확인하지 못해 수치를 표시하지 않습니다.',
  queueSize: 0,
  source: 'fallback',
  status: 'degraded',
}

export function useRiotApiStatus() {
  return useQuery<RiotApiStatus>({
    queryKey: ['riot', 'api-status'],
    queryFn: async () => {
      try {
        const status = await getRiotApiStatus()

        return {
          ...status,
          source: 'api',
        }
      } catch {
        return fallbackRiotApiStatus
      }
    },
    placeholderData: fallbackRiotApiStatus,
    retry: false,
    staleTime: 1000 * 60,
    refetchInterval: 1000 * 60 * 2,
  })
}
