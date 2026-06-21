import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { sendAiChatMessage, type AiChatContext, type AiChatMessage } from '../../../api/aiChatApi'

export function useAiChat(context?: AiChatContext) {
  const [messages, setMessages] = useState<AiChatMessage[]>([])

  const mutation = useMutation({
    mutationFn: (userText: string) => {
      const next: AiChatMessage[] = [...messages, { role: 'user', content: userText }]
      return sendAiChatMessage({ messages: next, context }).then((reply) => {
        return { userText, reply }
      })
    },
    onSuccess: ({ userText, reply }) => {
      setMessages((prev) => [
        ...prev,
        { role: 'user', content: userText },
        { role: 'assistant', content: reply },
      ])
    },
  })

  const send = (text: string) => {
    const trimmed = text.trim()
    if (!trimmed || mutation.isPending) return
    mutation.mutate(trimmed)
  }

  const reset = () => {
    setMessages([])
    mutation.reset()
  }

  return {
    messages,
    send,
    reset,
    isPending: mutation.isPending,
    isError: mutation.isError,
  }
}
