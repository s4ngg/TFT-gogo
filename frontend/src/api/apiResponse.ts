export interface ApiResponse<T> {
  data: T
  message?: string
  success: boolean
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function unwrapApiResponse<T>(payload: ApiResponse<T> | T): T {
  if (isRecord(payload) && 'success' in payload && 'data' in payload) {
    return payload.data as T
  }

  return payload as T
}
