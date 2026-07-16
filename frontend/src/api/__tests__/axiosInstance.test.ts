import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import axiosInstance, { resolveApiBaseUrl } from '../axiosInstance'
import useAuthStore from '../../store/useAuthStore'

interface RetriableAdapterConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const originalAdapter = axiosInstance.defaults.adapter
const originalAxiosAdapter = axios.defaults.adapter

function createResponse(
  config: InternalAxiosRequestConfig,
  data: unknown,
): AxiosResponse {
  return {
    config,
    data,
    headers: {},
    status: 200,
    statusText: 'OK',
  }
}

function createUnauthorizedError(config: InternalAxiosRequestConfig): AxiosError {
  const response: AxiosResponse = {
    config,
    data: {
      message: '인증이 필요합니다.',
      success: false,
    },
    headers: {},
    status: 401,
    statusText: 'Unauthorized',
  }

  return new AxiosError('Unauthorized', undefined, config, undefined, response)
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  axios.defaults.adapter = originalAxiosAdapter
  useAuthStore.getState().clearAuth()
})

describe('resolveApiBaseUrl', () => {
  it('환경변수가 없으면 기본 /api를 사용한다', () => {
    assert.equal(resolveApiBaseUrl({}), '/api')
  })

  it('환경변수가 공백이면 기본 /api를 사용한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiBaseUrl: '   ',
        apiUrl: '',
      }),
      '/api',
    )
  })

  it('VITE_API_URL을 VITE_API_BASE_URL보다 우선한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiBaseUrl: 'https://fallback.example.com/api',
        apiUrl: 'https://primary.example.com/api',
      }),
      'https://primary.example.com/api',
    )
  })

  it('현재 프론트 origin만 지정되면 /api로 보정한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000',
        currentOrigin: 'http://localhost:3000',
      }),
      '/api',
    )
  })

  it('현재 프론트 origin에 trailing slash가 있어도 /api로 보정한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000/',
        currentOrigin: 'http://localhost:3000',
      }),
      '/api',
    )
  })

  it('현재 origin이라도 /api 경로가 명시되어 있으면 유지한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:3000/api',
        currentOrigin: 'http://localhost:3000',
      }),
      'http://localhost:3000/api',
    )
  })

  it('별도 API origin의 /api 경로는 유지한다', () => {
    assert.equal(
      resolveApiBaseUrl({
        apiUrl: 'http://localhost:8080/api',
        currentOrigin: 'http://localhost:3000',
      }),
      'http://localhost:8080/api',
    )
  })
})

describe('401 token refresh', () => {
  it('동시 401 응답은 한 번만 갱신하고 새 토큰으로 원 요청을 재시도한다', async () => {
    let protectedRequestCount = 0
    let refreshRequestCount = 0
    const retryAuthorizationHeaders: string[] = []

    axiosInstance.defaults.adapter = async (config) => {
      protectedRequestCount += 1

      if (!(config as RetriableAdapterConfig)._retry) {
        throw createUnauthorizedError(config)
      }

      retryAuthorizationHeaders.push(String(config.headers.Authorization))
      return createResponse(config, { success: true })
    }
    axios.defaults.adapter = async (config) => {
      refreshRequestCount += 1
      return createResponse(config, {
        data: {
          accessToken: 'refreshed-token',
          user: {
            email: 'sojung@example.com',
            nickname: '소정',
          },
        },
        success: true,
      })
    }
    useAuthStore.getState().setAuth({ token: 'expired-token' })

    await Promise.all([
      axiosInstance.get('/v1/members/me'),
      axiosInstance.get('/v1/members/me'),
    ])

    assert.equal(protectedRequestCount, 4)
    assert.equal(refreshRequestCount, 1)
    assert.deepEqual(retryAuthorizationHeaders, [
      'Bearer refreshed-token',
      'Bearer refreshed-token',
    ])
    assert.equal(useAuthStore.getState().token, 'refreshed-token')
  })

  it('401 이후 토큰 갱신이 실패하면 저장된 인증 정보를 정리한다', async () => {
    axiosInstance.defaults.adapter = async (config) => {
      throw createUnauthorizedError(config)
    }
    axios.defaults.adapter = async (config) => {
      throw createUnauthorizedError(config)
    }
    useAuthStore.getState().setAuth({ token: 'expired-token' })

    await assert.rejects(() => axiosInstance.get('/v1/members/me'))

    assert.equal(useAuthStore.getState().token, null)
  })
})
