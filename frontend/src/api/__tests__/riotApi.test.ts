import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'
import { getRiotApiStatus } from '../riotApi'

interface RequestCall {
  method?: string
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function createStatusAdapter(responseData: unknown): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      method: config.method,
      url: config.url,
    })

    return {
      config,
      data: responseData,
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

describe('riotApi', () => {
  it('getRiotApiStatus는 Riot 상태 API 경로의 data를 반환한다', async () => {
    // given
    axiosInstance.defaults.adapter = createStatusAdapter({
      data: {
        activeConnections: 0,
        checkedAt: '2026-06-14T00:00:00Z',
        message: 'Riot API 요청 대기열이 비어 있습니다.',
        queueSize: 0,
        status: 'available',
      },
      success: true,
    })

    // when
    const response = await getRiotApiStatus()

    // then
    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/riot/status')
    assert.equal(response.status, 'available')
    assert.equal(response.queueSize, 0)
  })

  it('getRiotApiStatus는 실패 응답 원문을 노출하지 않는 도메인 에러를 던진다', async () => {
    // given
    axiosInstance.defaults.adapter = createStatusAdapter({
      message: 'internal-token-like-detail',
      success: false,
    })

    // when & then
    await assert.rejects(getRiotApiStatus(), (error: unknown) => {
      assert.ok(error instanceof Error)
      assert.equal(error.message, 'Riot API 상태 조회 실패')
      assert.equal(error.message.includes('internal-token-like-detail'), false)

      return true
    })
  })
})
