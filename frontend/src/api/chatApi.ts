import axiosInstance from './axiosInstance'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'

const API_BASE_URL = import.meta.env?.VITE_API_URL || import.meta.env?.VITE_API_BASE_URL || '/api'

export interface ChatMessage {
  createdAt: string
  id: string
  message: string
  roomId: string
  senderName: string
  senderTier: string
  sequence: number
}

interface RawChatMessage extends Omit<ChatMessage, 'senderTier'> {
  senderTier?: null | string
}

export interface SendChatMessageRequest {
  message: string
  senderName: string
  senderTier?: string
}

export type ChatStreamPayload = ApiResponse<RawChatMessage> | RawChatMessage

function encodeRoomId(roomId: string) {
  return encodeURIComponent(roomId)
}

function getApiBaseUrl() {
  return API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
}

export function buildChatStreamUrl(roomId: string) {
  return `${getApiBaseUrl()}/v1/chat/rooms/${encodeRoomId(roomId)}/stream`
}

export function isChatMessage(value: unknown): value is RawChatMessage {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.roomId === 'string' &&
    typeof value.senderName === 'string' &&
    (
      value.senderTier === undefined ||
      value.senderTier === null ||
      typeof value.senderTier === 'string'
    ) &&
    typeof value.message === 'string' &&
    typeof value.createdAt === 'string' &&
    typeof value.sequence === 'number'
  )
}

function normalizeChatMessage(message: RawChatMessage): ChatMessage {
  return {
    ...message,
    senderTier: message.senderTier ?? 'Unranked',
  }
}

function parseChatMessage(value: unknown) {
  return isChatMessage(value) ? normalizeChatMessage(value) : null
}

export function parseChatStreamMessage(value: unknown) {
  const payload = unwrapApiResponse(value as ChatStreamPayload)
  return parseChatMessage(payload)
}

export async function getChatMessages(roomId: string) {
  try {
    const response = await axiosInstance.get<ApiResponse<unknown>>(
      `/v1/chat/rooms/${encodeRoomId(roomId)}/messages`,
    )
    const payload = unwrapApiResponse(response.data)

    if (!Array.isArray(payload)) {
      return []
    }

    return payload
      .map((message) => parseChatMessage(message))
      .filter((message): message is ChatMessage => message !== null)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`채팅 메시지 조회 실패: ${message}`)
  }
}

export async function sendChatMessage(roomId: string, request: SendChatMessageRequest) {
  try {
    const response = await axiosInstance.post<ApiResponse<unknown>>(
      `/v1/chat/rooms/${encodeRoomId(roomId)}/messages`,
      request,
    )
    const message = parseChatMessage(unwrapApiResponse(response.data))

    if (!message) {
      throw new Error('채팅 메시지 응답 형식이 올바르지 않습니다.')
    }

    return message
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`채팅 메시지 전송 실패: ${message}`)
  }
}
