import {
  Bot,
  MessageCircle,
  RotateCcw,
  Send,
  X,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import type {
  GameGuideAiPathfinderPhase,
  GameGuideAiPathfinderRef,
  GameGuideAiPathfinderResponse,
} from '../../../api/gameGuideAiPathfinderApi'
import type { GuideTab } from '../../../api/guide'
import { useGameGuideAiPathfinder } from '../hooks/useGameGuideAiPathfinder'
import styles from '../Guide.module.css'

interface GameGuideAiChatWidgetProps {
  activeTab: GuideTab
  activeTabLabel: string
  candidateRefs: GameGuideAiPathfinderRef[]
  isOpen: boolean
  onGuideJump: (tab: GuideTab, query: string, label?: string) => void
  onOpenChange: (isOpen: boolean) => void
  patchVersion: string
  selectedRefs: GameGuideAiPathfinderRef[]
}

const GUIDE_TAB_BY_REF_TYPE: Record<GameGuideAiPathfinderRef['guideType'], GuideTab> = {
  AUGMENT: 'augments',
  CHAMPION: 'champions',
  ITEM: 'items',
  TRAIT: 'traits',
}

const PHASE_LABELS: Record<GameGuideAiPathfinderPhase, string> = {
  ANY: '공통',
  EARLY: '초반',
  LATE: '후반',
  MID: '중반',
}

interface ResultBlockProps {
  onGuideJump: (tab: GuideTab, query: string, label?: string) => void
  response: GameGuideAiPathfinderResponse
}

function ResultBlock({ onGuideJump, response }: ResultBlockProps) {
  function handleRefClick(ref: GameGuideAiPathfinderRef) {
    const label = ref.name?.trim() || ref.targetKey
    onGuideJump(GUIDE_TAB_BY_REF_TYPE[ref.guideType], label, label)
  }

  function renderRefButton(ref: GameGuideAiPathfinderRef, suffix?: string) {
    return (
      <button
        key={`${ref.guideType}-${ref.targetKey}-${suffix ?? 'ref'}`}
        onClick={() => handleRefClick(ref)}
        type="button"
      >
        {ref.name ?? ref.targetKey}
      </button>
    )
  }

  return (
    <div className={styles.gameGuideAiResult}>
      <strong>{response.title}</strong>
      <p>{response.summary}</p>

      {response.phasePlan.length > 0 && (
        <div className={styles.gameGuideAiSection}>
          {response.phasePlan.map((phase) => (
            <article key={`${phase.phase}-${phase.title}`}>
              <span>{PHASE_LABELS[phase.phase]}</span>
              <div>
                <b>{phase.title}</b>
                <p>{phase.description}</p>
                {phase.guideRefs.length > 0 && (
                  <div className={styles.gameGuideAiPhaseRefs}>
                    {phase.guideRefs.map((ref) => renderRefButton(ref, phase.title))}
                  </div>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      {response.recommendedRefs.length > 0 && (
        <div className={styles.gameGuideAiRecommendedList}>
          {response.recommendedRefs.map((ref) => (
            <div key={`${ref.guideType}-${ref.targetKey}`}>
              {renderRefButton(ref, 'recommended')}
              <small>{ref.reason}</small>
            </div>
          ))}
        </div>
      )}

      {response.avoidMistakes.length > 0 && (
        <ul className={styles.gameGuideAiList}>
          {response.avoidMistakes.map((mistake) => (
            <li key={mistake}>{mistake}</li>
          ))}
        </ul>
      )}

      {response.isFallback && (
        <span className={styles.gameGuideAiFallbackBadge}>기본 응답</span>
      )}

      {response.sourceRefs.length > 0 && (
        <div className={styles.gameGuideAiMetaBlock}>
          <span>참고 항목</span>
          <div className={styles.gameGuideAiRefList}>
            {response.sourceRefs.map((ref) => renderRefButton(ref, 'source'))}
          </div>
        </div>
      )}

      {response.limitations.length > 0 && (
        <div className={styles.gameGuideAiMetaBlock}>
          <span>응답 기준</span>
          <ul className={styles.gameGuideAiList}>
            {response.limitations.map((limitation) => (
              <li key={limitation}>{limitation}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function GameGuideAiChatWidget({
  activeTab,
  activeTabLabel,
  candidateRefs,
  isOpen,
  onGuideJump,
  onOpenChange,
  patchVersion,
  selectedRefs,
}: GameGuideAiChatWidgetProps) {
  const [input, setInput] = useState('')
  const messagesRef = useRef<HTMLDivElement>(null)
  const {
    isPending,
    messages,
    reset,
    sendQuestion,
  } = useGameGuideAiPathfinder()
  const selectedRefLabel = selectedRefs[0]?.name?.trim()

  useEffect(() => {
    const messagesElement = messagesRef.current
    if (!messagesElement) return

    messagesElement.scrollTop = messagesElement.scrollHeight
  }, [messages, isPending])

  function submitQuestion() {
    const question = input.trim()
    if (!question || isPending) return

    sendQuestion({
      activeTab,
      candidateRefs,
      patchVersion,
      question,
      selectedRefs,
    })
    setInput('')
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    submitQuestion()
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey) return

    event.preventDefault()
    submitQuestion()
  }

  return (
    <aside className={styles.gameGuideAiShell} aria-label="GameGuide AI 챗봇">
      {isOpen ? (
        <section className={styles.gameGuideAiPanel}>
          <header className={styles.gameGuideAiHeader}>
            <div>
              <span>
                <Bot size={15} />
                GameGuide AI
              </span>
              <small>{patchVersion} · {activeTabLabel}</small>
            </div>
            <div className={styles.gameGuideAiHeaderActions}>
              {messages.length > 0 && (
                <button aria-label="대화 초기화" onClick={reset} type="button">
                  <RotateCcw size={15} />
                </button>
              )}
              <button aria-label="챗봇 닫기" onClick={() => onOpenChange(false)} type="button">
                <X size={16} />
              </button>
            </div>
          </header>

          {selectedRefs.length > 0 && (
            <div className={styles.gameGuideAiSelectedRefs}>
              {selectedRefs.map((ref) => (
                <span key={`${ref.guideType}-${ref.targetKey}`}>
                  {ref.name ?? ref.targetKey}
                </span>
              ))}
            </div>
          )}

          <div className={styles.gameGuideAiMessages} ref={messagesRef}>
            {messages.length === 0 && (
              <div className={styles.gameGuideAiEmpty}>
                <MessageCircle size={20} />
                <p>{activeTabLabel}에 대해 물어보세요.</p>
              </div>
            )}

            {messages.map((message) => (
              <div
                className={message.role === 'user'
                  ? styles.gameGuideAiUserMessage
                  : styles.gameGuideAiAssistantMessage}
                key={message.id}
              >
                {message.response ? (
                  <ResultBlock onGuideJump={onGuideJump} response={message.response} />
                ) : (
                  <p>{message.content}</p>
                )}
              </div>
            ))}

            {isPending && (
              <div className={styles.gameGuideAiAssistantMessage}>
                <p className={styles.gameGuideAiTyping}>
                  <span />
                  <span />
                  <span />
                </p>
              </div>
            )}
          </div>

          <form className={styles.gameGuideAiForm} onSubmit={handleSubmit}>
            <textarea
              aria-label="GameGuide AI 질문"
              disabled={isPending}
              maxLength={500}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={selectedRefLabel
                ? `${selectedRefLabel} 질문 입력`
                : `${activeTabLabel} 질문 입력`}
              rows={2}
              value={input}
            />
            <button
              aria-label="질문 전송"
              disabled={isPending || !input.trim()}
              type="submit"
            >
              <Send size={16} />
            </button>
          </form>
        </section>
      ) : (
        <button
          aria-label="GameGuide AI 챗봇 열기"
          className={styles.gameGuideAiLauncher}
          onClick={() => onOpenChange(true)}
          type="button"
        >
          <Bot size={24} />
          <span>AI</span>
        </button>
      )}
    </aside>
  )
}

export default GameGuideAiChatWidget
