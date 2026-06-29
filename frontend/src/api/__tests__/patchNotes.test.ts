import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse } from 'axios'

import { getPatchChangeImageUrl } from '../../pages/PatchNotes/patchNotesImages'
import axiosInstance from '../axiosInstance'
import { getPatchNotes, sanitizePatchHighlight, type PatchChange, type PatchNoteDetail } from '../patchNotes'
import { readPatchChangeStatsPayload } from '../patchNoteStatsPayload'

const originalAdapter = axiosInstance.defaults.adapter

function createAdapter(responseData: unknown): AxiosAdapter {
  return async (config): Promise<AxiosResponse> => ({
    config,
    data: {
      data: responseData,
      success: true,
    },
    headers: {},
    status: 200,
    statusText: 'OK',
  })
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
})

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

const fallbackPatchNotes: PatchNoteDetail[] = [
  {
    changes: [],
    date: '2026.05.12',
    description: 'fallback description',
    focus: 'fallback focus',
    highlights: [],
    imageUrl: '/fallback.png',
    status: '이전',
    title: '17.3 패치',
    version: '17.3',
  },
]

describe('getPatchNotes', () => {
  it('strips Riot count prefixes from patch highlights', async () => {
    axiosInstance.defaults.adapter = createAdapter([
      {
        description: 'summary',
        highlights: [
          'Traits > (6) Animal Squad',
          '(5) N.O.V.A. additional effects',
        ],
        isCurrent: true,
        publishedAt: '2026-06-09T18:00:00',
        summary: 'summary',
        title: '17.6 Patch Notes',
        version: '17.6',
      },
    ])

    const response = await getPatchNotes(fallbackPatchNotes)

    assert.deepEqual(response.data[0]?.highlights, ['Animal Squad', 'N.O.V.A. additional effects'])
    assert.equal(sanitizePatchHighlight('(6) Animal Squad'), 'Animal Squad')
  })

  it('크롤링 목차 경로로 들어온 focus와 highlights를 사용자용 요약 문구로 정리한다', async () => {
    axiosInstance.defaults.adapter = createAdapter([
      {
        description: '공식 패치 요약입니다.',
        focus: '추가 패치 노트 > 밸런스 변경 사항',
        highlights: [
          '추가 패치 노트 > 밸런스 변경 사항',
          '체계 > 시작 조우자',
          '체계 > 시작 조우자',
        ],
        isCurrent: true,
        publishedAt: '2026-06-09T18:00:00',
        summary: '공식 패치 요약입니다.',
        title: '전략적 팀 전투 17.5 패치',
        version: '17.5',
      },
    ])

    const response = await getPatchNotes(fallbackPatchNotes)
    const patchNote = response.data[0]

    assert.equal(response.source, 'api')
    assert.equal(patchNote?.summary, '공식 패치 요약입니다.')
    assert.equal(patchNote?.focus, '공식 패치 요약입니다.')
    assert.deepEqual(patchNote?.highlights, ['밸런스 변경 사항', '시작 조우자'])
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
