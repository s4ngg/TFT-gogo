import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { readPatchChangeStatsPayload } from '../patchNoteStatsPayload'

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

    assert.equal(result, payload.stats)
    assert.equal(stats.totalChanges, 12)
    assert.deepEqual(stats.categoryCounts, { CHAMPION: 5 })
    assert.deepEqual(stats.typeCounts, { BUFF: 7 })
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

    assert.equal(result, payload)
    assert.equal(stats.totalChanges, 8)
    assert.deepEqual(stats.categoryCounts, { ITEM: 3 })
    assert.deepEqual(stats.typeCounts, { NERF: 2 })
  })

  it('payload가 null 또는 undefined이면 원본 값을 반환한다', () => {
    assert.equal(readPatchChangeStatsPayload(null), null)
    assert.equal(readPatchChangeStatsPayload(undefined), undefined)
  })

  it('payload가 객체가 아니면 원본 값을 반환한다', () => {
    const textPayload = 'stats'
    const arrayPayload = ['stats']

    assert.equal(readPatchChangeStatsPayload(textPayload), textPayload)
    assert.equal(readPatchChangeStatsPayload(arrayPayload), arrayPayload)
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

    assert.equal(result, payload)
    assert.equal(stats.totalChanges, 5)
    assert.deepEqual(stats.categoryCounts, { CHAMPION: 2 })
    assert.deepEqual(stats.typeCounts, { BUFF: 1 })
  })
})
