import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { getPatchChangeImageUrl } from '../../pages/PatchNotes/patchNotesImages'
import type { PatchChange } from '../patchNotes'
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

function patchChange(overrides: Partial<PatchChange> = {}): PatchChange {
  return {
    after: '',
    before: '',
    category: '챔피언',
    id: 1,
    impact: '중간',
    summary: '',
    tags: [],
    target: '카이사',
    type: '조정',
    ...overrides,
  }
}

describe('getPatchChangeImageUrl', () => {
  it('backend imageUrl이 있으면 fallback보다 우선한다', () => {
    const imageUrl = getPatchChangeImageUrl(patchChange({ imageUrl: '  https://example.com/kaisa.png  ' }))

    assert.equal(imageUrl, 'https://example.com/kaisa.png')
  })

  it('챔피언 fallback은 CDragon square helper 경로를 사용한다', () => {
    const imageUrl = getPatchChangeImageUrl(patchChange({ target: '카이사 샘플 1' }))

    assert.ok(imageUrl.includes('/characters/tft17_kaisa/hud/tft17_kaisa_square.tft_set17.png'))
  })

  it('아이템 fallback은 중앙 fallback item set 설정을 사용한다', () => {
    const imageUrl = getPatchChangeImageUrl(patchChange({
      category: '아이템',
      target: '구인수의 격노검',
    }))

    assert.ok(imageUrl.endsWith('/tft_item_guinsoosrageblade.tft_set13.png'))
  })
})
