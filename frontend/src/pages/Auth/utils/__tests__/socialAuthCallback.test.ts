import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { parseSocialAuthCallback, readSocialAuthErrorCode } from '../socialAuthCallback'

describe('parseSocialAuthCallback', () => {
  it('fragment의 accessToken을 우선 파싱한다', () => {
    const payload = parseSocialAuthCallback({
      hash: '#accessToken=access-token',
      search: '?accessToken=query-token',
    })

    assert.equal(payload.token, 'access-token')
  })

  it('jwt alias도 파싱한다', () => {
    const payload = parseSocialAuthCallback({
      hash: '#jwt=jwt-token',
      search: '',
    })

    assert.equal(payload.token, 'jwt-token')
  })

  it('토큰이 없으면 오류를 던진다', () => {
    assert.throws(
      () => parseSocialAuthCallback({ hash: '', search: '?accessToken=query-token' }),
      /access token/,
    )
  })

  it('허용된 OAuth 오류 코드를 읽는다', () => {
    assert.equal(readSocialAuthErrorCode('?oauthError=email_exists'), 'email_exists')
    assert.equal(readSocialAuthErrorCode('?oauthError=email_required'), 'email_required')
  })

  it('알 수 없는 OAuth 오류 코드는 provider_error로 처리한다', () => {
    assert.equal(readSocialAuthErrorCode('?oauthError=unknown'), 'provider_error')
    assert.equal(readSocialAuthErrorCode(''), 'provider_error')
  })
})
