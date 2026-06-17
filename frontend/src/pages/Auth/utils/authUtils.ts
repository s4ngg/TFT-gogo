export function parseSummonerTag(value: string) {
  const [summonerName = '', tagLine = ''] = value.split('#').map((part) => part.trim())

  return {
    summonerName: summonerName || undefined,
    tagLine: tagLine || undefined,
  }
}

export function mapAuthError(error: unknown, isSignup: boolean): string {
  const message = error instanceof Error ? error.message : String(error)
  const normalizedMessage = message.toLowerCase()

  if (normalizedMessage.includes('network') || normalizedMessage.includes('timeout')) {
    return '서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (
    normalizedMessage.includes('404') ||
    normalizedMessage.includes('not found')
  ) {
    return '인증 API 경로를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (
    normalizedMessage.includes('401') ||
    normalizedMessage.includes('unauthorized') ||
    normalizedMessage.includes('login failed')
  ) {
    return '이메일 또는 비밀번호가 올바르지 않습니다.'
  }

  if (
    normalizedMessage.includes('409') ||
    normalizedMessage.includes('conflict') ||
    normalizedMessage.includes('already') ||
    normalizedMessage.includes('duplicate')
  ) {
    return '이미 사용 중인 이메일입니다.'
  }

  if (normalizedMessage.includes('signup failed')) {
    return '회원가입 정보를 다시 확인해 주세요.'
  }

  return isSignup
    ? '회원가입 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    : '로그인 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}

export function mapOAuthErrorCode(code: string | null): string {
  if (!code) {
    return ''
  }

  if (code === 'email_exists') {
    return '같은 이메일로 가입된 계정이 있습니다. 이메일 로그인을 이용해 주세요.'
  }

  if (code === 'email_required') {
    return '소셜 계정에서 이메일 정보를 확인할 수 없습니다.'
  }

  return '소셜 로그인 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}

export function mapSocialAuthError(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error)
  const normalizedMessage = message.toLowerCase()

  if (normalizedMessage.includes('network') || normalizedMessage.includes('timeout')) {
    return '서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (normalizedMessage.includes('404') || normalizedMessage.includes('not found')) {
    return '소셜 로그인 API 경로를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (
    normalizedMessage.includes('503') ||
    normalizedMessage.includes('service unavailable') ||
    normalizedMessage.includes('social_provider_not_configured')
  ) {
    return '해당 소셜 로그인은 현재 사용할 수 없습니다. 이메일 로그인을 이용해 주세요.'
  }

  return '소셜 로그인 시작 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}
