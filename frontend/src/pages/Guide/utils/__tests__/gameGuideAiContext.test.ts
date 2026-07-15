import assert from 'node:assert/strict'
import test from 'node:test'
import type { GameGuideAiPathfinderRef } from '../../../../api/gameGuideAiPathfinderApi'
import {
  createGameGuideAiScopeKey,
  createGameGuideAiWidgetKey,
  readGameGuideAiScopedValue,
} from '../gameGuideAiContext'

const selectedRef: GameGuideAiPathfinderRef = {
  guideType: 'TRAIT',
  name: 'Animal Squad',
  targetKey: 'TFT17_AnimalSquad',
}

test('same patch and tab keep the scoped AI value', () => {
  const scopeKey = createGameGuideAiScopeKey('17.3', 'traits')

  assert.deepEqual(
    readGameGuideAiScopedValue({ scopeKey, value: [selectedRef] }, scopeKey, []),
    [selectedRef],
  )
})

test('tab change immediately hides previous AI refs', () => {
  const previousScopeKey = createGameGuideAiScopeKey('17.3', 'traits')
  const currentScopeKey = createGameGuideAiScopeKey('17.3', 'items')

  assert.deepEqual(
    readGameGuideAiScopedValue(
      { scopeKey: previousScopeKey, value: [selectedRef] },
      currentScopeKey,
      [],
    ),
    [],
  )
})

test('patch change immediately hides previous AI refs', () => {
  const previousScopeKey = createGameGuideAiScopeKey('17.2', 'traits')
  const currentScopeKey = createGameGuideAiScopeKey('17.3', 'traits')

  assert.deepEqual(
    readGameGuideAiScopedValue(
      { scopeKey: previousScopeKey, value: [selectedRef] },
      currentScopeKey,
      [],
    ),
    [],
  )
})

test('widget key changes for both tab and patch transitions', () => {
  const originalKey = createGameGuideAiWidgetKey('17.3:traits', [selectedRef])

  assert.notEqual(originalKey, createGameGuideAiWidgetKey('17.3:items', [selectedRef]))
  assert.notEqual(originalKey, createGameGuideAiWidgetKey('17.4:traits', [selectedRef]))
})
