import { isRecord } from './apiResponse'

export function readPatchChangeStatsPayload(payload: unknown): unknown {
  if (!isRecord(payload)) return payload
  return isRecord(payload.stats) ? payload.stats : payload
}
