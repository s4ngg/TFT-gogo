import axiosInstance from './axiosInstance'
import type { ApiResponse } from './apiResponse'
import { unwrapApiResponse } from './apiResponse'

export type SocialProvider = 'google' | 'kakao' | 'naver'

interface RawSocialLoginStartResponse {
  authorizationUrl?: string
}

export interface SocialLoginStartResponse {
  authorizationUrl: string
}

function normalizeSocialLoginStartResponse(
  payload: RawSocialLoginStartResponse,
): SocialLoginStartResponse {
  if (!payload.authorizationUrl) {
    throw new Error('Social login start response does not include an authorizationUrl')
  }

  return {
    authorizationUrl: payload.authorizationUrl,
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
