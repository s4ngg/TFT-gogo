import assert from 'node:assert/strict'
import { test } from 'node:test'
import { GameGuideAiPathfinderError } from '../../../../api/gameGuideAiPathfinderApi'
import {
  createGameGuideAiConversationHistory,
  createGameGuideAiFallbackResponse,
  getGameGuideAiExplicitErrorMessage,
  type GameGuideAiChatMessage,
} from '../useGameGuideAiPathfinder'

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

test('GameGuide AI conversation history keeps recent compact messages', () => {
  const longContent = '아이템을 더 자세히 설명해줘 '.repeat(80)
  const messages: GameGuideAiChatMessage[] = Array.from({ length: 8 }, (_, index) => ({
    content: index === 7 ? longContent : `message-${index + 1}`,
    id: index + 1,
    role: index % 2 === 0 ? 'user' : 'assistant',
  }))

  const history = createGameGuideAiConversationHistory(messages)

  assert.equal(history.length, 6)
  assert.equal(history[0].content, 'message-3')
  assert.equal(history[5].role, 'assistant')
  assert.ok(history[5].content.endsWith('...'))
  assert.ok(history[5].content.length <= 703)
})

test('GameGuide AI explicit error message exposes auth and rate limit errors', () => {
  assert.equal(
    getGameGuideAiExplicitErrorMessage(
      new GameGuideAiPathfinderError('로그인이 필요합니다.', 'AUTH_REQUIRED', 401),
    ),
    '로그인이 필요합니다.',
  )
  assert.equal(
    getGameGuideAiExplicitErrorMessage(
      new GameGuideAiPathfinderError(
        'GameGuide AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.',
        'RATE_LIMITED',
        429,
      ),
    ),
    'GameGuide AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.',
  )
})

test('GameGuide AI explicit error message keeps request failures on fallback path', () => {
  assert.equal(
    getGameGuideAiExplicitErrorMessage(
      new GameGuideAiPathfinderError('GameGuide AI 요청 중 오류가 발생했습니다.', 'REQUEST_FAILED'),
    ),
    undefined,
  )
  assert.equal(getGameGuideAiExplicitErrorMessage(new Error('network')), undefined)
})
