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
  for (const provider of ['google', 'naver'] as const) {
    it(`${provider} 소셜 로그인 시작 URL을 인증 스펙 경로로 요청한다`, async () => {
      // given
      const authorizationUrl = `http://localhost:8080/oauth2/authorization/${provider}`
      axiosInstance.defaults.adapter = createSocialAuthAdapter(authorizationUrl)
      const { getSocialLoginStart } = await import('../socialAuth')

      // when
      const response = await getSocialLoginStart(provider)

      // then
      assert.equal(requestCalls[0]?.method, 'get')
      assert.equal(requestCalls[0]?.url, `/v1/auth/social/${provider}`)
      assert.equal(response.authorizationUrl, authorizationUrl)
    })
  }

  it('소셜 로그인 시작 응답의 URL 앞뒤 공백은 제거한 뒤 정규화한다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter(
      '  https://api.example.com/oauth2/authorization/google  ',
    )
    const { getSocialLoginStart } = await import('../socialAuth')

    // when
    const response = await getSocialLoginStart('google')

    // then
    assert.equal(response.authorizationUrl, 'https://api.example.com/oauth2/authorization/google')
  })

  it('소셜 로그인 시작 응답에 authorizationUrl이 없으면 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter()
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('naver'), /authorizationUrl/)
  })

  it('소셜 로그인 시작 응답의 authorizationUrl이 공백뿐이면 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter('   ')
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /authorizationUrl/)
  })

  it('상대 경로 authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter('/oauth2/authorization/google')
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /invalid authorizationUrl/)
  })

  it('protocol-relative authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter('//localhost:8080/oauth2/authorization/google')
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /invalid authorizationUrl/)
  })

  it('http_https가_아닌 authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter('javascript:alert(1)')
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /unsupported authorizationUrl/)
  })

  it('data_scheme authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter('data:text/plain,token')
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /unsupported authorizationUrl/)
  })

  it('userinfo가_포함된 authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter(
      'https://user:pass@example.com/oauth2/authorization/google',
    )
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /unsupported authorizationUrl/)
  })

  it('중간 공백이 포함된 authorizationUrl은 오류를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createSocialAuthAdapter(
      'https://api.example.com/oauth2/authorization/goo gle',
    )
    const { getSocialLoginStart } = await import('../socialAuth')

    // when, then
    await assert.rejects(() => getSocialLoginStart('google'), /invalid authorizationUrl/)
  })
})
