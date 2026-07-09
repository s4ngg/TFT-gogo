import assert from 'node:assert/strict'
import test from 'node:test'
import { isGuideHighlighted } from '../guideHighlight'

test('isGuideHighlighted matches the guide name on the active tab', () => {
  assert.equal(
    isGuideHighlighted(
      'traits',
      { name: '동물특공대', targetKey: 'TFT17_AnimalSquad' },
      { label: '동물특공대', query: '동물특공대', tab: 'traits' },
    ),
    true,
  )
})

test('isGuideHighlighted matches targetKey when the AI ref has no display name', () => {
  assert.equal(
    isGuideHighlighted(
      'champions',
      { name: '징크스', targetKey: 'TFT17_Jinx' },
      { label: 'TFT17_Jinx', query: 'TFT17_Jinx', tab: 'champions' },
    ),
    true,
  )
})

test('isGuideHighlighted ignores matching names on a different tab', () => {
  assert.equal(
    isGuideHighlighted(
      'items',
      { name: '구인수의 격노검', targetKey: 'TFT_Item_GuinsoosRageblade' },
      { label: '구인수의 격노검', query: '구인수의 격노검', tab: 'champions' },
    ),
    false,
  )
})
