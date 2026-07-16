import assert from 'node:assert/strict'
import test from 'node:test'
import type { TraitGuide } from '../../../../api/guide'
import { createGameGuideAiCandidateRefs } from '../useGameGuideAiCandidateRefs'

function trait(name: string, targetKey?: string): TraitGuide {
  return {
    champions: [],
    count: 1,
    iconUrl: '',
    levels: [],
    name,
    summary: '',
    targetKey,
    tips: [],
    type: 'origin',
  }
}

test('fallback entries without targetKey do not become AI candidates', () => {
  const refs = createGameGuideAiCandidateRefs('traits', [
    trait('Fallback One'),
    trait('Fallback Two', '   '),
  ])

  assert.deepEqual(refs, [])
})

test('candidate refs keep real keys and remove duplicates', () => {
  const refs = createGameGuideAiCandidateRefs('traits', [
    trait('Animal Squad', 'TFT17_AnimalSquad'),
    trait('Animal Squad Duplicate', 'TFT17_AnimalSquad'),
  ])

  assert.deepEqual(refs, [{
    guideType: 'TRAIT',
    name: 'Animal Squad',
    targetKey: 'TFT17_AnimalSquad',
  }])
})

test('candidate refs keep the backend request limit', () => {
  const entries = Array.from(
    { length: 21 },
    (_, index) => trait(`Trait ${index}`, `TFT17_Trait_${index}`),
  )

  assert.equal(createGameGuideAiCandidateRefs('traits', entries).length, 20)
})
