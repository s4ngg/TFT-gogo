import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { normalizeAuthResponse, normalizeMemberResponse } from '../memberApiPayload'

describe('memberApiPayload', () => {
  it('인증 응답의 accessToken과 user payload를 정규화한다', () => {
    const response = normalizeAuthResponse(
      {
        accessToken: 'access-token',
        user: {
          email: 'sojung@example.com',
          nickname: '소정',
        },
      },
      'fallback@example.com',
    )

    assert.equal(response.token, 'access-token')
    assert.equal(response.user.email, 'sojung@example.com')
    assert.equal(response.user.nickname, '소정')
  })

  it('인증 응답의 token/member 조합과 fallback email을 유지한다', () => {
    const response = normalizeAuthResponse(
      {
        member: {
          nickname: '소정',
        },
        token: 'access-token',
      },
      'fallback@example.com',
    )

    assert.equal(response.token, 'access-token')
    assert.equal(response.user.email, 'fallback@example.com')
    assert.equal(response.user.nickname, '소정')
  })

  it('토큰이 없으면 인증 응답 오류를 던진다', () => {
    assert.throws(
      () => normalizeAuthResponse({ user: { email: 'sojung@example.com' } }, 'fallback@example.com'),
      /token/,
    )
  })

  it('회원 응답의 nullable profileImage와 false notificationEnabled를 보존한다', () => {
    const response = normalizeMemberResponse({
      email: 'sojung@example.com',
      id: 1,
      nickname: null,
      notificationEnabled: false,
      profileImage: null,
    })

    assert.equal(response.email, 'sojung@example.com')
    assert.equal(response.id, 1)
    assert.equal(response.nickname, undefined)
    assert.equal(response.profileImage, null)
    assert.equal(response.notificationEnabled, false)
  })

  it('회원 응답에 email이 없으면 오류를 던진다', () => {
    assert.throws(() => normalizeMemberResponse({ id: 1 }), /email/)
  })
})
