import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'

interface RequestCall {
  data?: unknown
  method?: string
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function createAuthAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: config.data,
      method: config.method,
      url: config.url,
    })

    return {
      config,
      data: {
        data: {
          accessToken: 'access-token',
          user: {
            email: 'sojung@example.com',
            nickname: '소정',
          },
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

describe('memberApi', () => {
  it('login은 인증 스펙 경로로 요청한다', async () => {
    // given
    axiosInstance.defaults.adapter = createAuthAdapter()
    const { login } = await import('../memberApi')

    // when
    const response = await login({
      email: 'sojung@example.com',
      password: 'password123',
    })

    // then
    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/v1/auth/login')
    assert.equal(response.token, 'access-token')
    assert.equal(response.user.email, 'sojung@example.com')
  })

  it('signup은 인증 스펙 경로로 요청한다', async () => {
    // given
    axiosInstance.defaults.adapter = createAuthAdapter()
    const { signup } = await import('../memberApi')

    // when
    const response = await signup({
      email: 'sojung@example.com',
      password: 'password123',
      tagLine: 'KR1',
    })

    // then
    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/v1/auth/signup')
    assert.equal(response.token, 'access-token')
    assert.equal(response.user.email, 'sojung@example.com')
  })
})
