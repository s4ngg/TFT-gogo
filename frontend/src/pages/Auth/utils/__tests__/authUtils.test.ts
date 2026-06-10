import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { mapAuthError } from '../authUtils'

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
