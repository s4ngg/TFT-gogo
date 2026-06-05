import { describe, expect, it } from 'vitest'

import { readPatchChangeStatsPayload } from '../patchNotes'

function toRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    throw new Error('expected object payload')
  }

  return value as Record<string, unknown>
}

describe('readPatchChangeStatsPayload', () => {
  it('payload.stats 객체가 있으면 nested stats를 반환한다', () => {
    const payload = {
      stats: {
        categoryCounts: {
          CHAMPION: 5,
        },
        totalChanges: 12,
        typeCounts: {
          BUFF: 7,
        },
      },
      totalChanges: 99,
    }

    const result = readPatchChangeStatsPayload(payload)
    const stats = toRecord(result)

    expect(result).toBe(payload.stats)
    expect(stats.totalChanges).toBe(12)
    expect(stats.categoryCounts).toEqual({ CHAMPION: 5 })
    expect(stats.typeCounts).toEqual({ BUFF: 7 })
  })

  it('payload.stats가 없으면 top-level payload를 반환한다', () => {
    const payload = {
      categoryCounts: {
        ITEM: 3,
      },
      totalChanges: 8,
      typeCounts: {
        NERF: 2,
      },
    }

    const result = readPatchChangeStatsPayload(payload)
    const stats = toRecord(result)

    expect(result).toBe(payload)
    expect(stats.totalChanges).toBe(8)
    expect(stats.categoryCounts).toEqual({ ITEM: 3 })
    expect(stats.typeCounts).toEqual({ NERF: 2 })
  })

  it('payload가 null 또는 undefined이면 원본 값을 반환한다', () => {
    expect(readPatchChangeStatsPayload(null)).toBe(null)
    expect(readPatchChangeStatsPayload(undefined)).toBe(undefined)
  })

  it('payload가 객체가 아니면 원본 값을 반환한다', () => {
    const textPayload = 'stats'
    const arrayPayload = ['stats']

    expect(readPatchChangeStatsPayload(textPayload)).toBe(textPayload)
    expect(readPatchChangeStatsPayload(arrayPayload)).toBe(arrayPayload)
  })

  it('payload.stats가 객체가 아니면 top-level payload를 반환한다', () => {
    const payload = {
      categoryCounts: {
        CHAMPION: 2,
      },
      stats: null,
      totalChanges: 5,
      typeCounts: {
        BUFF: 1,
      },
    }

    const result = readPatchChangeStatsPayload(payload)
    const stats = toRecord(result)

    expect(result).toBe(payload)
    expect(stats.totalChanges).toBe(5)
    expect(stats.categoryCounts).toEqual({ CHAMPION: 2 })
    expect(stats.typeCounts).toEqual({ BUFF: 1 })
  })
})
