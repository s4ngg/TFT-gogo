import useAuthStore from '../../../store/useAuthStore'

interface PartyAuthUser {
  id?: number | string
  nickname?: string
  summonerName?: string
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readLegacyAuthUser(value: unknown): PartyAuthUser | null {
  if (!isRecord(value) || !isRecord(value.user)) {
    return null
  }

  return value.user
}

function readUserId(value: number | string | undefined): string | null {
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  if (typeof value === 'string') {
    const normalized = value.trim()
    if (normalized.length > 0) return normalized
  }
  return null
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')

  return globalThis.atob(padded)
}

function readJwtSubject(token: string | null): string | null {
  if (!token) {
    return null
  }

  const [, payload] = token.split('.')

  if (!payload) {
    return null
  }

  try {
    const parsed: unknown = JSON.parse(decodeBase64Url(payload))

    if (isRecord(parsed) && typeof parsed.sub === 'string' && parsed.sub.trim()) {
      return parsed.sub.trim()
    }
  } catch {
    return null
  }

  return null
}

export function usePartyAuth() {
  const token = useAuthStore((state) => state.token)
  const legacyUser = useAuthStore((state) => readLegacyAuthUser(state))

  return {
    displayName: legacyUser?.nickname ?? legacyUser?.summonerName ?? '나',
    token,
    userId: readUserId(legacyUser?.id) ?? readJwtSubject(token),
  }
}
