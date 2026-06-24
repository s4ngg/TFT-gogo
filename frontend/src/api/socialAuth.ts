import axiosInstance from './axiosInstance'
import type { ApiResponse } from './apiResponse'
import { unwrapApiResponse } from './apiResponse'

export type SocialProvider = 'google' | 'naver'

interface RawSocialLoginStartResponse {
  authorizationUrl?: string
}

export interface SocialLoginStartResponse {
  authorizationUrl: string
}

const MISSING_AUTHORIZATION_URL_MESSAGE =
  'Social login start response does not include an authorizationUrl'
const INVALID_AUTHORIZATION_URL_MESSAGE =
  'Social login start response includes an invalid authorizationUrl'
const UNSUPPORTED_AUTHORIZATION_URL_MESSAGE =
  'Social login start response includes an unsupported authorizationUrl'

function normalizeAuthorizationUrl(value: string | undefined): string {
  if (value === undefined || value.trim().length === 0) {
    throw new Error(MISSING_AUTHORIZATION_URL_MESSAGE)
  }

  const trimmedValue = value.trim()
  if (/\s/.test(trimmedValue)) {
    throw new Error(INVALID_AUTHORIZATION_URL_MESSAGE)
  }

  let authorizationUrl: URL
  try {
    authorizationUrl = new URL(trimmedValue)
  } catch {
    throw new Error(INVALID_AUTHORIZATION_URL_MESSAGE)
  }

  if (authorizationUrl.protocol !== 'http:' && authorizationUrl.protocol !== 'https:') {
    throw new Error(UNSUPPORTED_AUTHORIZATION_URL_MESSAGE)
  }

  if (authorizationUrl.username || authorizationUrl.password) {
    throw new Error(UNSUPPORTED_AUTHORIZATION_URL_MESSAGE)
  }

  return authorizationUrl.toString()
}

function normalizeSocialLoginStartResponse(
  payload: RawSocialLoginStartResponse,
): SocialLoginStartResponse {
  return {
    authorizationUrl: normalizeAuthorizationUrl(payload.authorizationUrl),
  }
}

export async function getSocialLoginStart(provider: SocialProvider): Promise<SocialLoginStartResponse> {
  try {
    const response = await axiosInstance.get<
      RawSocialLoginStartResponse | ApiResponse<RawSocialLoginStartResponse>
    >(`/v1/auth/social/${provider}`)
    const payload = unwrapApiResponse(response.data)

    return normalizeSocialLoginStartResponse(payload)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    throw new Error(`Social login start failed: ${message}`)
  }
}
