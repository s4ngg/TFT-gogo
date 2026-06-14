import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'

interface RequestCall {
  method?: string
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function createSocialAuthAdapter(authorizationUrl?: string): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      method: config.method,
      url: config.url,
    })

    return {
      config,
      data: {
        data: {
          authorizationUrl,
        },
        success: true,
      },
      headers: {},
      status: 200,
      statusText: 'OK',
    }
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  requestCalls.length = 0
})

describe('socialAuth', () => {
  it('provider별 소셜 로그인 시작 URL을 인증 스펙 경로로 요청한다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter(
      'http://localhost:8080/oauth2/authorization/google',
    )
    const { getSocialLoginStart } = await import('../socialAuth')

    // when
    const response = await getSocialLoginStart('google')

    // then
    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/v1/auth/social/google')
    assert.equal(response.authorizationUrl, 'http://localhost:8080/oauth2/authorization/google')
  })

  it('소셜 로그인 시작 응답에 authorizationUrl이 없으면 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter()
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('kakao'), /authorizationUrl/)
  })
})
