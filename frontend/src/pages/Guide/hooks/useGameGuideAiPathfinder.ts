import { useMutation } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import {
  GameGuideAiPathfinderError,
  requestGameGuideAiPathfinder,
  type GameGuideAiConversationMessage,
  type GameGuideAiPathfinderErrorCode,
  type GameGuideAiPathfinderRequest,
  type GameGuideAiPathfinderRef,
  type GameGuideAiPathfinderResponse,
} from '../../../api/gameGuideAiPathfinderApi'
import type { GuideTab } from '../../../api/guide'

export interface GameGuideAiChatMessage {
  content: string
  id: number
  response?: GameGuideAiPathfinderResponse
  role: 'assistant' | 'user'
}

interface SendGameGuideAiQuestionParams {
  activeTab: GuideTab
  candidateRefs: GameGuideAiPathfinderRef[]
  patchVersion: string
  question: string
  selectedRefs: GameGuideAiPathfinderRef[]
}

interface PendingGameGuideAiQuestionParams extends SendGameGuideAiQuestionParams {
  conversationHistory: GameGuideAiConversationMessage[]
  epoch: number
}

const FALLBACK_TITLE_BY_TAB: Record<GuideTab, string> = {
  augments: '증강체 가이드 질문',
  champions: '챔피언 가이드 질문',
  items: '아이템 가이드 질문',
  traits: '시너지 가이드 질문',
}

const MAX_CONVERSATION_HISTORY = 6
const MAX_CONVERSATION_CONTENT_LENGTH = 700
const EXPLICIT_ERROR_CODES: ReadonlySet<GameGuideAiPathfinderErrorCode> = new Set([
  'AUTH_REQUIRED',
  'RATE_LIMITED',
])

function getSelectedRefLabel(selectedRefs: GameGuideAiPathfinderRef[]) {
  return selectedRefs[0]?.name?.trim() || selectedRefs[0]?.targetKey.trim() || ''
}

function truncateConversationContent(content: string) {
  const trimmed = content.trim()
  if (trimmed.length <= MAX_CONVERSATION_CONTENT_LENGTH) {
    return trimmed
  }

  return `${trimmed.slice(0, MAX_CONVERSATION_CONTENT_LENGTH).trimEnd()}...`
}

function getConversationMessageContent(message: GameGuideAiChatMessage) {
  if (!message.response) {
    return message.content
  }

  const response = message.response
  return [
    response.title,
    response.summary,
    ...response.evidenceNotes,
    ...response.creativeSuggestions,
    ...response.phasePlan.map((phase) => `${phase.title}: ${phase.description}`),
  ].join(' ')
}

export function createGameGuideAiConversationHistory(
  messages: GameGuideAiChatMessage[],
): GameGuideAiConversationMessage[] {
  return messages
    .filter((message) => getConversationMessageContent(message).trim().length > 0)
    .slice(-MAX_CONVERSATION_HISTORY)
    .map((message) => ({
      content: truncateConversationContent(getConversationMessageContent(message)),
      role: message.role,
    }))
}

export function createGameGuideAiFallbackResponse({
  activeTab,
  question,
  selectedRefs,
}: Pick<SendGameGuideAiQuestionParams, 'activeTab' | 'question' | 'selectedRefs'>): GameGuideAiPathfinderResponse {
  const selectedRefLabel = getSelectedRefLabel(selectedRefs)
  const title = selectedRefLabel
    ? `${selectedRefLabel} 가이드 질문`
    : FALLBACK_TITLE_BY_TAB[activeTab]

  return {
    avoidMistakes: [
      '현재 단계에서는 실제 메타 수치나 승률을 판단하지 않습니다.',
      '가이드 데이터에 없는 아이템, 증강체, 전적 정보는 단정하지 않습니다.',
    ],
    coreConcepts: [
      '질문한 키워드를 현재 가이드 탭에서 먼저 검색해 관련 항목을 확인하세요.',
      '시너지, 챔피언, 아이템, 증강체를 하나씩 연결해서 운영 흐름을 좁히는 방식이 안전합니다.',
    ],
    creativeSuggestions: [],
    evidenceNotes: [
      '현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다.',
    ],
    isFallback: true,
    limitations: [
      '일시적인 오류로 GameGuide AI 기본 안내를 표시합니다.',
      ...(selectedRefLabel ? [`선택 항목: ${selectedRefLabel}`] : []),
      `질문: ${question}`,
    ],
    phasePlan: [
      {
        description: '현재 탭에서 질문 키워드와 가장 가까운 가이드 항목을 먼저 확인합니다.',
        guideRefs: [],
        phase: 'ANY',
        title: '가이드 항목 확인',
      },
      {
        description: '연관된 시너지, 챔피언, 아이템을 순서대로 비교하며 다음 질문을 좁힙니다.',
        guideRefs: [],
        phase: 'ANY',
        title: '연관 정보 비교',
      },
    ],
    recommendedRefs: [],
    sourceRefs: selectedRefs,
    summary: selectedRefLabel
      ? `${selectedRefLabel} 기준의 기본 안내만 표시합니다.`
      : 'GameGuide AI 응답을 일시적으로 생성하지 못해 현재 가이드 화면 기준의 기본 안내만 표시합니다.',
    title,
  }
}

export function getGameGuideAiExplicitErrorMessage(error: unknown) {
  if (
    error instanceof GameGuideAiPathfinderError
    && EXPLICIT_ERROR_CODES.has(error.code)
  ) {
    return error.message
  }

  return undefined
}

export function useGameGuideAiPathfinder() {
  const [messages, setMessages] = useState<GameGuideAiChatMessage[]>([])
  const nextMessageId = useRef(1)
  const resetEpoch = useRef(0)

  function createMessage(
    role: GameGuideAiChatMessage['role'],
    content: string,
    response?: GameGuideAiPathfinderResponse,
  ): GameGuideAiChatMessage {
    const id = nextMessageId.current
    nextMessageId.current += 1

    return {
      content,
      id,
      response,
      role,
    }
  }

  const mutation = useMutation({
    mutationFn: async (params: PendingGameGuideAiQuestionParams) => {
      const request: GameGuideAiPathfinderRequest = {
        activeTab: params.activeTab,
        candidateRefs: params.candidateRefs,
        conversationHistory: params.conversationHistory,
        mode: 'AUTO',
        patchVersion: params.patchVersion,
        question: params.question,
        selectedRefs: params.selectedRefs,
      }
      const response = await requestGameGuideAiPathfinder(request)

      return {
        epoch: params.epoch,
        params,
        response,
      }
    },
    onError: (error, params) => {
      if (params.epoch !== resetEpoch.current) return

      const explicitErrorMessage = getGameGuideAiExplicitErrorMessage(error)
      if (explicitErrorMessage) {
        setMessages((current) => [
          ...current,
          createMessage('assistant', explicitErrorMessage),
        ])
        return
      }

      const fallback = createGameGuideAiFallbackResponse(params)
      setMessages((current) => [
        ...current,
        createMessage('assistant', fallback.summary, fallback),
      ])
    },
    onSuccess: ({ epoch, response }) => {
      if (epoch !== resetEpoch.current) return

      setMessages((current) => [
        ...current,
        createMessage('assistant', response.summary, response),
      ])
    },
  })

  function sendQuestion(params: SendGameGuideAiQuestionParams) {
    const question = params.question.trim()
    if (!question || mutation.isPending) return

    setMessages((current) => [
      ...current,
      createMessage('user', question),
    ])
    mutation.mutate({
      ...params,
      conversationHistory: createGameGuideAiConversationHistory(messages),
      epoch: resetEpoch.current,
      question,
    })
  }

  function reset() {
    resetEpoch.current += 1
    setMessages([])
    mutation.reset()
  }

  return {
    isError: mutation.isError,
    isPending: mutation.isPending,
    messages,
    reset,
    sendQuestion,
  }
}
