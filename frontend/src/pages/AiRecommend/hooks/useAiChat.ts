import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { sendChatMessage, type ChatContext, type ChatMessage } from '../../../api/chatApi'

export function useAiChat(context?: ChatContext) {
  const [messages, setMessages] = useState<ChatMessage[]>([])

  const mutation = useMutation({
    mutationFn: (userText: string) => {
      const next: ChatMessage[] = [...messages, { role: 'user', content: userText }]
      return sendChatMessage({ messages: next, context }).then((reply) => {
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
