import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import axios, {
  AxiosError,
  type AxiosAdapter,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import axiosInstance from '../axiosInstance'
import {
  GameGuideAiPathfinderError,
  requestGameGuideAiPathfinder,
  type GameGuideAiPathfinderRequest,
} from '../gameGuideAiPathfinderApi'
import useAuthStore from '../../store/useAuthStore'

const originalAdapter = axiosInstance.defaults.adapter
const originalAxiosAdapter = axios.defaults.adapter

const request: GameGuideAiPathfinderRequest = {
  activeTab: 'traits',
  candidateRefs: [],
  conversationHistory: [],
  mode: 'AUTO',
  patchVersion: '15.10',
  question: '도전자 운영 알려줘',
  selectedRefs: [],
}

function createErrorAdapter(status: number, message: string): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    const response: AxiosResponse = {
      config,
      data: {
        data: null,
        message,
        success: false,
      },
      headers: {},
      status,
      statusText: message,
    }

    return Promise.reject(new AxiosError(message, undefined, config, undefined, response))
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  axios.defaults.adapter = originalAxiosAdapter
  useAuthStore.getState().clearAuth()
})

describe('gameGuideAiPathfinderApi', () => {
  it('requestGameGuideAiPathfinder는 401을 로그인 필요 오류로 보존한다', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(401, '로그인이 필요합니다.')
    axios.defaults.adapter = createErrorAdapter(401, 'refresh failed')

    await assert.rejects(
      () => requestGameGuideAiPathfinder(request),
      (error: unknown) => {
        assert.ok(error instanceof GameGuideAiPathfinderError)
        assert.equal(error.code, 'AUTH_REQUIRED')
        assert.equal(error.status, 401)
        assert.equal(error.message, '로그인이 필요합니다.')

        return true
      },
    )
  })

  it('requestGameGuideAiPathfinder는 429를 요청 제한 오류로 보존한다', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(429, '요청 한도를 초과했습니다.')

    await assert.rejects(
      () => requestGameGuideAiPathfinder(request),
      (error: unknown) => {
        assert.ok(error instanceof GameGuideAiPathfinderError)
        assert.equal(error.code, 'RATE_LIMITED')
        assert.equal(error.status, 429)
        assert.equal(error.message, '요청 한도를 초과했습니다.')

        return true
      },
    )
  })
})
