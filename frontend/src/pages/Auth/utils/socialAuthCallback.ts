interface SocialAuthCallbackLocation {
  hash: string
  search: string
}

export interface SocialAuthCallbackPayload {
  token: string
}

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
