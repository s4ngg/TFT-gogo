import { useEffect, useRef, useState } from 'react'
import { Bot, Send, RotateCcw } from 'lucide-react'
import type { AiChatContext } from '../../../api/aiChatApi'
import { useAiChat } from '../hooks/useAiChat'
import useAuthStore from '../../../store/useAuthStore'
import styles from '../AiRecommend.module.css'

interface Props {
  context?: AiChatContext
}

function AiChat({ context }: Props) {
  const token = useAuthStore((state) => state.token)
  const { messages, send, reset, isPending, isError } = useAiChat(context)
  const [input, setInput] = useState('')
  const messagesRef = useRef<HTMLDivElement>(null)
  const lastUserBubbleRef = useRef<HTMLDivElement>(null)
  const lastUserIndex = messages.reduce((acc, msg, i) => (msg.role === 'user' ? i : acc), -1)
  const prevLengthRef = useRef(messages.length)

  useEffect(() => {
    const last = messages[messages.length - 1]
    const container = messagesRef.current
    const bubble = lastUserBubbleRef.current
    if (messages.length > prevLengthRef.current && last?.role === 'user' && container && bubble) {
      const offset = bubble.getBoundingClientRect().top - container.getBoundingClientRect().top
      container.scrollTo({ top: container.scrollTop + offset, behavior: 'smooth' })
    }
    prevLengthRef.current = messages.length
  }, [messages])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    send(input)
    setInput('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send(input)
      setInput('')
    }
  }

  return (
    <section className={`${styles.panel} ${styles.chatAiPanel}`}>
      <div className={styles.panelHead}>
        <Bot size={17} />
        <h2>AI 어시스턴트</h2>
        {messages.length > 0 && (
          <button type="button" className={styles.chatResetBtn} onClick={reset} aria-label="대화 초기화">
            <RotateCcw size={14} />
          </button>
        )}
      </div>

      <div ref={messagesRef} className={styles.chatMessages}>
        {!token && (
          <p className={styles.chatEmptyHint}>
            로그인 후 AI 어시스턴트를 이용할 수 있습니다.
          </p>
        )}

        {token && messages.length === 0 && (
          <p className={styles.chatEmptyHint}>
            유닛, 시너지, 증강체, 내 전적에 대해 뭐든 물어보세요.
          </p>
        )}

        {messages.map((msg, i) => (
          <div
            key={i}
            ref={i === lastUserIndex ? lastUserBubbleRef : undefined}
            className={msg.role === 'user' ? styles.chatBubbleUser : styles.chatBubbleAssistant}
          >
            {msg.role === 'assistant' && (
              <span className={styles.chatAssistantLabel}>AI</span>
            )}
            <p className={styles.chatBubbleText}>{msg.content}</p>
          </div>
        ))}

        {isPending && (
          <div className={styles.chatBubbleAssistant}>
            <span className={styles.chatAssistantLabel}>AI</span>
            <p className={`${styles.chatBubbleText} ${styles.chatTyping}`}>
              <span />
              <span />
              <span />
            </p>
          </div>
        )}

        {isError && (
          <p className={styles.chatErrorMsg}>
            응답 중 오류가 발생했습니다. 다시 시도해 주세요.
          </p>
        )}

        <div />
      </div>

      <form className={styles.chatForm} onSubmit={handleSubmit}>
        <textarea
          className={styles.chatInput}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={token ? '질문을 입력하세요 (Enter로 전송, Shift+Enter로 줄바꿈)' : '로그인이 필요합니다'}
          rows={2}
          disabled={isPending || !token}
          maxLength={500}
        />
        <button
          type="submit"
          className={styles.chatSendBtn}
          disabled={isPending || !input.trim() || !token}
          aria-label="전송"
        >
          <Send size={16} />
        </button>
      </form>
    </section>
  )
}

export default AiChat
