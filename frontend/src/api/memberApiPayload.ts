import type { AuthUser } from '../types/auth'

export interface RawAuthResponse {
  accessToken?: string
  jwt?: string
  member?: Partial<AuthUser>
  token?: string
  user?: Partial<AuthUser>
}

export interface AuthResponse {
  token: string
  user: AuthUser
}

export interface MemberProfile extends AuthUser {
  profileImage?: string | null
  notificationEnabled?: boolean
}

export interface RawMemberResponse {
  email?: string
  id?: number | string
  nickname?: string | null
  notificationEnabled?: boolean
  profileImage?: string | null
}

export function normalizeAuthResponse(payload: RawAuthResponse, fallbackEmail: string): AuthResponse {
  const token = payload.token ?? payload.accessToken ?? payload.jwt

  if (!token) {
    throw new Error('Auth response does not include a token')
  }

  const userPayload = payload.user ?? payload.member ?? {}
  const email = userPayload.email ?? fallbackEmail

  return {
    token,
    user: {
      ...userPayload,
      email,
    },
  }
}

export function normalizeMemberResponse(payload: RawMemberResponse): MemberProfile {
  if (!payload.email) {
    throw new Error('Member response does not include an email')
  }

  return {
    email: payload.email,
    id: payload.id,
    nickname: payload.nickname ?? undefined,
    notificationEnabled: payload.notificationEnabled,
    profileImage: payload.profileImage,
  }
}
