import useAuthStore from '../store/useAuthStore'
import type { ApiResponse } from './apiResponse'
import { isRecord, unwrapApiResponse } from './apiResponse'
import axiosInstance from './axiosInstance'

export interface ChatMessage {
  content: string
  createdAt: string
  id: string
  roomId: string
  senderId?: number | string | null
  senderName: string
  tier?: string | null
}

export interface ChatMessageCreateRequest {
  content: string
  roomId: string
}

interface ChatStreamHandlers {
  onClose: () => void
  onError: (reason: ChatStreamErrorReason) => void
  onMessage: (message: ChatMessage) => void
  onOpen: () => void
  onSnapshot: (messages: ChatMessage[]) => void
  onUnauthorized: () => void
}

export interface ChatStreamSubscription {
  close: () => void
}

export type ChatStreamErrorReason = 'client' | 'parse' | 'server' | 'network'

interface ParsedSseEvent {
  data: string
  event: string
}

export async function getChatMessages(roomId: string): Promise<ChatMessage[]> {
  try {
    const response = await axiosInstance.get<ApiResponse<ChatMessage[]>>(
      `/community/chat/rooms/${encodeURIComponent(roomId)}/messages`,
    )

    return unwrapApiResponse(response.data)
  } catch {
    throw new Error('채팅 메시지를 불러오지 못했습니다.')
  }
}

export async function sendChatMessage(request: ChatMessageCreateRequest): Promise<ChatMessage> {
  try {
    const payload: ChatMessageCreateRequest = {
      content: request.content,
      roomId: request.roomId,
    }
    const response = await axiosInstance.post<ApiResponse<ChatMessage>>(
      '/community/chat/messages',
      payload,
    )

    return unwrapApiResponse(response.data)
  } catch {
    throw new Error('채팅 메시지 전송에 실패했습니다.')
  }
}

export function subscribeChatRoom(roomId: string, handlers: ChatStreamHandlers): ChatStreamSubscription {
  const controller = new AbortController()

  void readChatStream(roomId, controller, handlers)

  return {
    close: () => controller.abort(),
  }
}

async function readChatStream(
  roomId: string,
  controller: AbortController,
  handlers: ChatStreamHandlers,
) {
  try {
    const response = await fetch(buildChatStreamUrl(roomId), {
      credentials: 'include',
      headers: buildStreamHeaders(),
      signal: controller.signal,
    })

    if (response.status === 401) {
      useAuthStore.getState().clearAuth()
      handlers.onUnauthorized()
      return
    }

    if (!response.ok || !response.body) {
      handlers.onError(response.status >= 400 && response.status < 500 ? 'client' : 'server')
      return
    }

    handlers.onOpen()
    await readSseBody(response.body, handlers, controller)

    if (!controller.signal.aborted) {
      handlers.onClose()
    }
  } catch {
    if (!controller.signal.aborted) {
      handlers.onError('network')
    }
  }
}

function buildStreamHeaders(): Record<string, string> {
  const token = useAuthStore.getState().token
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  return headers
}

function buildChatStreamUrl(roomId: string) {
  const baseUrl = axiosInstance.defaults.baseURL ?? '/api'
  const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl

  return `${normalizedBaseUrl}/community/chat/rooms/${encodeURIComponent(roomId)}/stream`
}

async function readSseBody(
  body: ReadableStream<Uint8Array>,
  handlers: ChatStreamHandlers,
  controller: AbortController,
) {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (!controller.signal.aborted) {
    const { done, value } = await reader.read()

    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const parsed = drainSseEvents(buffer)

    buffer = parsed.remaining
    for (const event of parsed.events) {
      const handled = handleSseEvent(event, handlers)

      if (!handled) {
        controller.abort()
        break
      }
    }
  }
}

function drainSseEvents(buffer: string) {
  const events: ParsedSseEvent[] = []
  let remaining = buffer
  let separatorIndex = findEventSeparator(remaining)

  while (separatorIndex >= 0) {
    const eventBlock = remaining.slice(0, separatorIndex)
    const separatorLength = remaining.startsWith('\r\n\r\n', separatorIndex) ? 4 : 2
    const event = parseSseEvent(eventBlock)

    if (event) {
      events.push(event)
    }

    remaining = remaining.slice(separatorIndex + separatorLength)
    separatorIndex = findEventSeparator(remaining)
  }

  return { events, remaining }
}

function findEventSeparator(value: string) {
  const crlfIndex = value.indexOf('\r\n\r\n')
  const lfIndex = value.indexOf('\n\n')

  if (crlfIndex === -1) {
    return lfIndex
  }

  if (lfIndex === -1) {
    return crlfIndex
  }

  return Math.min(crlfIndex, lfIndex)
}

function parseSseEvent(eventBlock: string): ParsedSseEvent | null {
  const dataLines: string[] = []
  let eventName = 'message'

  eventBlock.split(/\r?\n/).forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim()
      return
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  })

  if (dataLines.length === 0) {
    return null
  }

  return {
    data: dataLines.join('\n'),
    event: eventName,
  }
}

function handleSseEvent(event: ParsedSseEvent, handlers: ChatStreamHandlers) {
  let parsedPayload: unknown

  try {
    parsedPayload = JSON.parse(event.data)
  } catch {
    handlers.onError('parse')
    return false
  }

  if (event.event === 'snapshot') {
    const messages = Array.isArray(parsedPayload)
      ? parsedPayload.filter(isChatMessage)
      : []

    handlers.onSnapshot(messages)
    return true
  }

  if (event.event === 'message' && isChatMessage(parsedPayload)) {
    handlers.onMessage(parsedPayload)
  }

  return true
}

function isChatMessage(value: unknown): value is ChatMessage {
  return (
    isRecord(value)
    && typeof value.id === 'string'
    && typeof value.roomId === 'string'
    && (typeof value.senderId === 'number' || typeof value.senderId === 'string' || value.senderId == null)
    && typeof value.senderName === 'string'
    && (typeof value.tier === 'string' || value.tier == null)
    && typeof value.content === 'string'
    && typeof value.createdAt === 'string'
  )
}
