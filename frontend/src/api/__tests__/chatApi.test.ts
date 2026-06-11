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

function createChatAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: config.data,
      method: config.method,
      url: config.url,
    })

    return {
      config,
      data: {
        data: [
          {
            createdAt: '2026-06-11T09:00:00',
            id: 'party-recruitment-1',
            message: '안녕하세요',
            roomId: 'party-recruitment',
            senderName: '소정',
            senderTier: 'Diamond',
            sequence: 1,
          },
        ],
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

describe('chatApi', () => {
  it('getChatMessages는 채팅 스펙 경로로 요청한다', async () => {
    // given
    axiosInstance.defaults.adapter = createChatAdapter()
    const { getChatMessages } = await import('../chatApi')

    // when
    const response = await getChatMessages('party-recruitment')

    // then
    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/v1/chat/rooms/party-recruitment/messages')
    assert.equal(response[0]?.message, '안녕하세요')
    assert.equal(response[0]?.roomId, 'party-recruitment')
  })

  it('parseChatStreamMessage는 ApiResponse 이벤트 payload를 해석한다', async () => {
    // given
    const { parseChatStreamMessage } = await import('../chatApi')

    // when
    const response = parseChatStreamMessage({
      data: {
        createdAt: '2026-06-11T09:00:00',
        id: 'general-1',
        message: '안녕하세요',
        roomId: 'general',
        senderName: '소정',
        senderTier: null,
        sequence: 1,
      },
      success: true,
    })

    // then
    assert.equal(response?.id, 'general-1')
    assert.equal(response?.senderTier, 'Unranked')
  })
})
