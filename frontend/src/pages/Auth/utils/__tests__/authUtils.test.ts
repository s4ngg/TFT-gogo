import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { mapAuthError, mapOAuthErrorCode, mapSocialAuthError } from '../authUtils'

describe('mapAuthError', () => {
  it('404 라우팅 실패와 401 인증 실패 메시지를 구분한다', () => {
    assert.equal(
      mapAuthError(new Error('Login failed: Request failed with status code 404'), false),
      '인증 API 경로를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.',
    )

    assert.equal(
      mapAuthError(new Error('Login failed: Request failed with status code 401'), false),
      '이메일 또는 비밀번호가 올바르지 않습니다.',
    )
  })

  it('회원가입 409 중복 이메일 메시지를 유지한다', () => {
    assert.equal(
      mapAuthError(new Error('Signup failed: Request failed with status code 409'), true),
      '이미 사용 중인 이메일입니다.',
    )
  })
})

describe('mapOAuthErrorCode', () => {
  it('허용된 소셜 로그인 실패 코드를 사용자 메시지로 변환한다', () => {
    assert.equal(
      mapOAuthErrorCode('email_exists'),
      '같은 이메일로 가입된 계정이 있습니다. 이메일 로그인을 이용해 주세요.',
    )
    assert.equal(
      mapOAuthErrorCode('email_required'),
      '소셜 계정에서 이메일 정보를 확인할 수 없습니다.',
    )
    assert.equal(
      mapOAuthErrorCode('provider_error'),
      '소셜 로그인 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.',
    )
  })
})

describe('mapSocialAuthError', () => {
  it('소셜 로그인 시작 API의 네트워크와 404 오류를 구분한다', () => {
    assert.equal(
      mapSocialAuthError(new Error('Social login start failed: Network Error')),
      '서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
    )
    assert.equal(
      mapSocialAuthError(new Error('Social login start failed: Request failed with status code 404')),
      '소셜 로그인 API 경로를 찾을 수 없습니다. 잠시 후 다시 시도해 주세요.',
    )
  })

  it('소셜 provider 설정 누락 또는 503 오류를 안내 메시지로 변환한다', () => {
    const expectedMessage = '해당 소셜 로그인은 현재 사용할 수 없습니다. 이메일 로그인을 이용해 주세요.'

    assert.equal(
      mapSocialAuthError(new Error('Social login start failed: Request failed with status code 503')),
      expectedMessage,
    )
    assert.equal(
      mapSocialAuthError(new Error('Social login start failed: Service Unavailable')),
      expectedMessage,
    )
    assert.equal(
      mapSocialAuthError(new Error('Social login start failed: SOCIAL_PROVIDER_NOT_CONFIGURED')),
      expectedMessage,
    )
  })
})
