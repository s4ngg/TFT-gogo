import assert from 'node:assert/strict'
import test from 'node:test'

import {
  normalizeFavoriteChampions,
  normalizeRecentGuides,
  readFavoriteChampions,
  readRecentGuides,
  writeFavoriteChampions,
  writeRecentGuides,
} from '../guideQuickAccessStorage'

function createMemoryStorage(initialValues: Record<string, string | null> = {}) {
  const values = new Map(Object.entries(initialValues))

  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => {
      values.set(key, value)
    },
    values,
  }
}

test('normalizeFavoriteChampions removes invalid, blank, and duplicated values', () => {
  const result = normalizeFavoriteChampions([
    '아리',
    '  아리  ',
    '',
    '세트',
    123,
    '럭스',
  ])

  assert.deepEqual(result, ['아리', '세트', '럭스'])
})

test('normalizeRecentGuides keeps valid guide items and restores blank labels from query', () => {
  const result = normalizeRecentGuides([
    { label: '', query: '아리', tab: 'champions' },
    { label: '시너지', query: '도전자', tab: 'traits' },
    { label: '중복', query: '아리', tab: 'champions' },
    { label: '잘못된 탭', query: '아리', tab: 'unknown' },
    { label: '빈 검색어', query: ' ', tab: 'items' },
  ])

  assert.deepEqual(result, [
    { label: '아리', query: '아리', tab: 'champions' },
    { label: '시너지', query: '도전자', tab: 'traits' },
  ])
})

test('readFavoriteChampions and readRecentGuides safely recover from broken storage values', () => {
  const storage = createMemoryStorage({
    'tftgogo-guide-favorite-champions': '{broken',
    'tftgogo-guide-recent-guides': '{"not":"array"}',
  })

  assert.deepEqual(readFavoriteChampions(storage), [])
  assert.deepEqual(readRecentGuides(storage), [])
})

test('writeFavoriteChampions and writeRecentGuides persist normalized values', () => {
  const storage = createMemoryStorage()

  writeFavoriteChampions(['아리', '아리', '세트'], storage)
  writeRecentGuides([
    { label: '아리', query: '아리', tab: 'champions' },
    { label: '', query: '도전자', tab: 'traits' },
  ], storage)

  assert.equal(
    storage.values.get('tftgogo-guide-favorite-champions'),
    JSON.stringify(['아리', '세트']),
  )
  assert.equal(
    storage.values.get('tftgogo-guide-recent-guides'),
    JSON.stringify([
      { label: '아리', query: '아리', tab: 'champions' },
      { label: '도전자', query: '도전자', tab: 'traits' },
    ]),
  )
})
