import { useMutation } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { sendAiChatMessage, type AiChatContext, type AiChatMessage } from '../../../api/aiChatApi'

export function useAiChat(context?: AiChatContext) {
  const [messages, setMessages] = useState<AiChatMessage[]>([])
  const resetRef = useRef(0)

  const mutation = useMutation({
    mutationFn: ({ next, epoch }: { next: AiChatMessage[]; epoch: number }) => {
      return sendAiChatMessage({ messages: next, context }).then((reply) => {
        return { reply, epoch }
      })
    },
    onSuccess: ({ reply, epoch }) => {
      if (epoch !== resetRef.current) return
      setMessages((prev) => [...prev, { role: 'assistant', content: reply }])
    },
  })

  const send = (text: string) => {
    const trimmed = text.trim()
    if (!trimmed || mutation.isPending) return
    const epoch = resetRef.current
    const next: AiChatMessage[] = [...messages, { role: 'user', content: trimmed }]
    setMessages(next)
    mutation.mutate({ next, epoch })
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
