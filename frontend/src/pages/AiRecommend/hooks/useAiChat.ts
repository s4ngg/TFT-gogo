import { useMutation } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { sendAiChatMessage, type AiChatContext, type AiChatMessage } from '../../../api/aiChatApi'

export function useAiChat(context?: AiChatContext) {
  const [messages, setMessages] = useState<AiChatMessage[]>([])
  const resetRef = useRef(0)

  const mutation = useMutation({
    mutationFn: (userText: string) => {
      const epoch = resetRef.current
      const next: AiChatMessage[] = [...messages, { role: 'user', content: userText }]
      return sendAiChatMessage({ messages: next, context }).then((reply) => {
        return { userText, reply, epoch }
      })
    },
    onSuccess: ({ userText, reply, epoch }) => {
      if (epoch !== resetRef.current) return
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
    resetRef.current += 1
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
