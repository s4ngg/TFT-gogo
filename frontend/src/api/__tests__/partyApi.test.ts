import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'
import { cancelPartyJoin, createPartyPost, getPartyPosts, joinPartyPost } from '../partyApi'
import { COMMUNITY_PARTY_POSTS_QUERY_KEY, communityPartyPostsQueryKey } from '../partyQueryKeys'

interface RequestCall {
  data?: unknown
  method?: string
  params?: unknown
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

function createPartyAdapter(payload: unknown): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: config.data,
      method: config.method,
      params: config.params,
      url: config.url,
    })

    return {
      config,
      data: payload,
      headers: {},
      status: 200,
      statusText: 'OK',
    }
  }
}

function createFailingPartyAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: config.data,
      method: config.method,
      params: config.params,
      url: config.url,
    })

    throw new Error('network error')
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  requestCalls.length = 0
})

describe('partyApi', () => {
  it('파티 목록 query key는 전체 조회와 모드별 조회를 분리한다', () => {
    assert.deepEqual(communityPartyPostsQueryKey(), COMMUNITY_PARTY_POSTS_QUERY_KEY)
    assert.deepEqual(
      communityPartyPostsQueryKey({ mode: '랭크' }),
      ['community', 'parties', { mode: '랭크' }],
    )
  })

  it('getPartyPosts는 선택한 모드를 서버 필터 파라미터로 보낸다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [
        {
          chatRoomId: 'party-recruitment',
          content: '일반전 연습',
          currentMembers: 1,
          gameMode: 'NORMAL_TFT',
          id: 9,
          maxMembers: 4,
          tags: [],
          title: '일반 같이 해요',
        },
      ],
      success: true,
    })

    const response = await getPartyPosts({ mode: '일반' })

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/community/parties')
    assert.deepEqual(requestCalls[0]?.params, { mode: 'NORMAL_TFT' })
    assert.equal(response.source, 'api')
    assert.equal(response.data[0]?.mode, '일반')
  })

  it('getPartyPosts는 전체 조회일 때 모드 파라미터를 보내지 않는다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [],
      success: true,
    })

    await getPartyPosts()

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/community/parties')
    assert.equal(requestCalls[0]?.params, undefined)
  })

  it('getPartyPosts는 실패 응답에서 더미 모집글 대신 빈 unavailable 결과를 반환한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      success: false,
      message: '파티 목록 조회 실패',
    })

    const response = await getPartyPosts()

    assert.equal(response.source, 'unavailable')
    assert.deepEqual(response.data, [])
  })

  it('getPartyPosts는 잘못된 payload에서 더미 모집글 대신 빈 unavailable 결과를 반환한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: {
        id: 1,
        title: '배열이 아닌 payload',
      },
      success: true,
    })

    const response = await getPartyPosts()

    assert.equal(response.source, 'unavailable')
    assert.deepEqual(response.data, [])
  })

  it('getPartyPosts는 네트워크 오류에서 더미 모집글 대신 빈 unavailable 결과를 반환한다', async () => {
    axiosInstance.defaults.adapter = createFailingPartyAdapter()

    const response = await getPartyPosts()

    assert.equal(response.source, 'unavailable')
    assert.deepEqual(response.data, [])
  })

  it('createPartyPost는 파티 생성 스펙 경로와 payload로 요청한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: {
        chatRoomId: 'party-recruitment',
        content: '편하게 연습하실 분',
        currentMembers: 1,
        gameMode: 'NORMAL_TFT',
        id: 7,
        maxMembers: 4,
        tags: ['초보 환영', '연습'],
        tier: '제한 없음',
        title: '일반 같이 해요',
        userId: 3,
      },
      success: true,
    })

    const response = await createPartyPost({
      capacity: '1/4',
      deadline: '2026-06-16T21:00:00',
      description: '편하게 연습하실 분',
      mode: '일반',
      tags: ['초보 환영', '연습'],
      tier: '제한 없음',
      title: '일반 같이 해요',
    })
    const requestBody = readRequestData(requestCalls[0])

    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/community/parties')
    assert.equal(requestBody.content, '편하게 연습하실 분')
    assert.equal(requestBody.gameMode, 'NORMAL_TFT')
    assert.equal(requestBody.maxMembers, 4)
    assert.equal(response.id, '7')
    assert.equal(response.mode, '일반')
    assert.equal(response.capacity, '1/4')
  })

  it('파티 생성 응답 payload가 없으면 성공으로 처리하지 않는다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: null,
      success: true,
    })

    await assert.rejects(
      () =>
        createPartyPost({
          capacity: '1/2',
          deadline: '2026-06-16T21:00:00',
          description: '랭크 듀오 구합니다',
          mode: '랭크',
          tags: ['랭크'],
          tier: '마스터+',
          title: '마스터 듀오',
        }),
      /파티 모집글 등록 응답이 올바르지 않습니다/,
    )
  })

  it('파티 생성 응답에 식별자가 없으면 성공으로 처리하지 않는다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: {},
      success: true,
    })

    await assert.rejects(
      () =>
        createPartyPost({
          capacity: '1/2',
          deadline: '2026-06-16T21:00:00',
          description: '랭크 듀오 구합니다',
          mode: '랭크',
          tags: ['랭크'],
          tier: '마스터+',
          title: '마스터 듀오',
        }),
      /파티 모집글 등록 응답이 올바르지 않습니다/,
    )
  })

  it('파티 참여와 취소 응답 payload가 없으면 성공으로 처리하지 않는다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: null,
      success: true,
    })

    await assert.rejects(() => joinPartyPost('7'), /파티 참여 응답이 올바르지 않습니다/)
    await assert.rejects(() => cancelPartyJoin('7'), /파티 참여 취소 응답이 올바르지 않습니다/)
  })
})
