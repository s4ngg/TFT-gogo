export const MAX_CHAT_RECONNECT_ATTEMPTS = 3
export const CHAT_RECONNECT_DELAYS_MS = [1000, 2500, 5000] as const

export function shouldRetryChatConnection(attempt: number) {
  return Number.isInteger(attempt)
    && attempt >= 1
    && attempt <= MAX_CHAT_RECONNECT_ATTEMPTS
}

export function getChatReconnectDelay(attempt: number): number | null {
  return shouldRetryChatConnection(attempt)
    ? CHAT_RECONNECT_DELAYS_MS[attempt - 1] ?? null
    : null
}

export function getNextChatReconnectAttempt(currentAttempt: number): number | null {
  const nextAttempt = Math.floor(currentAttempt) + 1

  return shouldRetryChatConnection(nextAttempt) ? nextAttempt : null
}
