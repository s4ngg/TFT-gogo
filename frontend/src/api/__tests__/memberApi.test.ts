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

function readRequestData(call: RequestCall | undefined): Record<string, unknown> {
  if (!call) {
    throw new Error('request was not captured')
  }

  if (typeof call.data === 'string') {
    return JSON.parse(call.data) as Record<string, unknown>
  }

  if (typeof call.data === 'object' && call.data !== null) {
    return call.data as Record<string, unknown>
  }

  return {}
}

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

function createMemberAdapter(): AxiosAdapter {
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
          email: 'sojung@example.com',
          id: 1,
          nickname: '소정',
          notificationEnabled: false,
          profileImage: null,
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
      nickname: '소정',
      password: 'password123',
    })
    const requestBody = readRequestData(requestCalls[0])

    // then
    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/v1/auth/signup')
    assert.equal(requestBody.nickname, '소정')
    assert.equal('summonerName' in requestBody, false)
    assert.equal('tagLine' in requestBody, false)
    assert.equal(response.token, 'access-token')
    assert.equal(response.user.email, 'sojung@example.com')
  })

  it('getMe는 내 정보 조회 스펙 경로로 요청한다', async () => {
    // given
    axiosInstance.defaults.adapter = createMemberAdapter()
    const { getMe } = await import('../memberApi')

    // when
    const response = await getMe()

    // then
    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/v1/members/me')
    assert.equal(response.email, 'sojung@example.com')
    assert.equal(response.nickname, '소정')
    assert.equal(response.profileImage, null)
    assert.equal(response.notificationEnabled, false)
  })
})
