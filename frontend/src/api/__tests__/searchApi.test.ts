import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'
import { getSummonerProfile, SearchRateLimitError } from '../searchApi'

interface RequestCall {
  method?: string
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function createProfileAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      method: config.method,
      url: config.url,
    })

    return {
      config,
      data: {
        data: {
          gameName: 'Hide on Bush',
          leaguePoints: 0,
          losses: 0,
          profileIconId: 29,
          puuid: 'summoner-puuid',
          rank: null,
          summonerLevel: 100,
          tagLine: 'KR1',
          tier: null,
          wins: 0,
        },
        success: true,
      },
      headers: {},
      status: 200,
      statusText: 'OK',
    }
  }
}

function createErrorAdapter(status: number, headers: Record<string, unknown> = {}): AxiosAdapter {
  return async (): Promise<AxiosResponse> => {
    throw {
      message: 'Request failed',
      response: {
        headers,
        status,
      },
    }
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  requestCalls.length = 0
})

describe('searchApi', () => {
  it('getSummonerProfile은 소환사 프로필 스펙 경로로 요청한다', async () => {
    axiosInstance.defaults.adapter = createProfileAdapter()

    const response = await getSummonerProfile('Hide on Bush', 'KR1')

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/summoners/Hide%20on%20Bush/KR1')
    assert.equal(response.gameName, 'Hide on Bush')
    assert.equal(response.tier, null)
    assert.equal(response.wins, 0)
  })

  it('getSummonerProfile은 404를 소환사 없음 오류로 변환한다', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(404)

    await assert.rejects(getSummonerProfile('Unknown', 'KR1'), (error: unknown) => {
      assert.ok(error instanceof Error)
      assert.equal(error.message, 'NOT_FOUND')

      return true
    })
  })

  it('getSummonerProfile은 429의 Retry-After를 rate limit 오류에 보존한다', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(429, {
      'Retry-After': '42',
    })

    await assert.rejects(getSummonerProfile('Hide on Bush', 'KR1'), (error: unknown) => {
      assert.ok(error instanceof SearchRateLimitError)
      assert.equal(error.message, 'RATE_LIMITED')
      assert.equal(error.retryAfterSeconds, 42)

      return true
    })
  })

  it('getSummonerProfile은 429의 retry-after 소문자 헤더도 파싱한다', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(429, {
      'retry-after': '42',
    })

    await assert.rejects(getSummonerProfile('Hide on Bush', 'KR1'), (error: unknown) => {
      assert.ok(error instanceof SearchRateLimitError)
      assert.equal(error.retryAfterSeconds, 42)

      return true
    })
  })
})
