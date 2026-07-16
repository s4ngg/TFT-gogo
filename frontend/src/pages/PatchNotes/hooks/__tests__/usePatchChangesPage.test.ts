import assert from 'node:assert/strict'
import { test } from 'node:test'
import type { PatchChange, PatchNoteDetail } from '../../../../api/patchNotes'
import { resolvePatchChangesFallbackData } from '../usePatchChangesPage'
import { PATCH_CHANGE_PAGE_SIZE } from '../usePatchNotesPageState'

function patchChange(overrides: Partial<PatchChange> = {}): PatchChange {
  return {
    after: '',
    before: '',
    category: '챔피언',
    id: 1,
    impact: '중간',
    summary: 'fallback change',
    tags: [],
    target: '럭스',
    type: '조정',
    ...overrides,
  }
}

function patchNote(version: string, changes: PatchChange[]): PatchNoteDetail {
  return {
    changes,
    date: '2026.05.26',
    description: 'description',
    focus: 'focus',
    highlights: [],
    imageUrl: '/patch.png',
    status: '현재',
    title: `${version} 패치`,
    version,
  }
}

test('patch changes fallback uses static fallback when API history has no change rows', () => {
  const fallbackData = [patchNote('17.3', [patchChange()])]
  const patchHistory = [patchNote('17.3', [])]

  const result = resolvePatchChangesFallbackData('17.3', fallbackData, patchHistory)

  assert.equal(result, fallbackData)
})

test('patch changes fallback keeps API history when it already includes selected patch changes', () => {
  const fallbackData = [patchNote('17.3', [patchChange()])]
  const patchHistory = [patchNote('17.3', [patchChange({ target: '아펠리오스' })])]

  const result = resolvePatchChangesFallbackData('17.3', fallbackData, patchHistory)

  assert.equal(result, patchHistory)
})

test('patch changes fallback stays empty when requested version is absent from both sources', () => {
  const fallbackData = [patchNote('17.2', [patchChange()])]
  const patchHistory = [patchNote('17.1', [patchChange({ target: 'Aphelios' })])]

  const result = resolvePatchChangesFallbackData('17.3', fallbackData, patchHistory)

  assert.deepEqual(result, [])
})

test('patch changes page size follows the current public full patch request policy', () => {
  assert.equal(PATCH_CHANGE_PAGE_SIZE, 1000)
})
