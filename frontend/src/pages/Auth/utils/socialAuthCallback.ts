interface SocialAuthCallbackLocation {
  hash: string
  search: string
}

export interface SocialAuthCallbackPayload {
  token: string
}

export type SocialAuthErrorCode = 'email_exists' | 'email_required' | 'provider_error'

const socialAuthErrorCodes = new Set<SocialAuthErrorCode>([
  'email_exists',
  'email_required',
  'provider_error',
])

function readParam(params: URLSearchParams, names: string[]): string | null {
  for (const name of names) {
    const value = params.get(name)

    if (value && value.trim()) {
      return value.trim()
    }
  }

  return null
}

export function parseSocialAuthCallback(
  location: SocialAuthCallbackLocation,
): SocialAuthCallbackPayload {
  const hashParams = new URLSearchParams(location.hash.replace(/^#/, ''))
  const token = readParam(hashParams, ['accessToken', 'token', 'jwt'])

  if (!token) {
    throw new Error('Social auth callback does not include an access token')
  }

  return { token }
}

export function readSocialAuthErrorCode(search: string): SocialAuthErrorCode {
  const rawError = new URLSearchParams(search).get('oauthError')

  if (rawError && socialAuthErrorCodes.has(rawError as SocialAuthErrorCode)) {
    return rawError as SocialAuthErrorCode
  }

  return 'provider_error'
}
