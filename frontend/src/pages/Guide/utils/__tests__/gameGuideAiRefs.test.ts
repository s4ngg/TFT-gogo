import assert from 'node:assert/strict'
import test from 'node:test'
import { createGameGuideAiRef } from '../gameGuideAiRefs'

test('createGameGuideAiRef returns null when targetKey is missing', () => {
  assert.equal(createGameGuideAiRef('TRAIT', 'Animal Squad'), null)
  assert.equal(createGameGuideAiRef('ITEM', 'Infinity Edge', '   '), null)
})

test('createGameGuideAiRef preserves a real targetKey', () => {
  assert.deepEqual(
    createGameGuideAiRef('CHAMPION', '  Jinx  ', '  TFT17_Jinx  '),
    {
      guideType: 'CHAMPION',
      name: 'Jinx',
      targetKey: 'TFT17_Jinx',
    },
  )
})

test('createGameGuideAiRef rejects a blank display name', () => {
  assert.equal(createGameGuideAiRef('AUGMENT', '   ', 'TFT17_Augment_Test'), null)
})
