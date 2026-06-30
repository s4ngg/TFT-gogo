import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createGameGuideAiFallbackResponse } from '../useGameGuideAiPathfinder'

test('GameGuide AI fallback preserves selected guide refs', () => {
  const selectedRefs = [
    {
      guideType: 'TRAIT' as const,
      name: '도전자',
      targetKey: 'TFT17_Challenger',
    },
  ]

  const response = createGameGuideAiFallbackResponse({
    activeTab: 'traits',
    question: '운영 방법 알려줘',
    selectedRefs,
  })

  assert.equal(response.title, '도전자 가이드 질문')
  assert.equal(response.summary, '도전자 기준의 기본 안내만 표시합니다.')
  assert.deepEqual(response.evidenceNotes, ['현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다.'])
  assert.deepEqual(response.creativeSuggestions, [])
  assert.deepEqual(response.sourceRefs, selectedRefs)
  assert.ok(response.limitations.includes('선택 항목: 도전자'))
  assert.equal(response.isFallback, true)
})

test('GameGuide AI fallback uses active tab title without selected refs', () => {
  const response = createGameGuideAiFallbackResponse({
    activeTab: 'items',
    question: '누구한테 줘?',
    selectedRefs: [],
  })

  assert.equal(response.title, '아이템 가이드 질문')
  assert.deepEqual(response.sourceRefs, [])
  assert.equal(response.isFallback, true)
})
