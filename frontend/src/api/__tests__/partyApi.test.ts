import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'
import { cancelPartyJoin, createPartyPost, getPartyPosts, joinPartyPost } from '../partyApi'
import {
  COMMUNITY_PARTY_POSTS_QUERY_KEY,
  communityPartyPostsQueryKey,
  communityPartyPostsScopedQueryKey,
} from '../partyQueryKeys'

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

function createRejectingPartyAdapter(payload: unknown, status = 409): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: config.data,
      method: config.method,
      params: config.params,
      url: config.url,
    })

    return Promise.reject({
      response: {
        data: payload,
        status,
      },
    })
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
    assert.deepEqual(
      communityPartyPostsScopedQueryKey({ mode: '랭크' }, '7'),
      ['community', 'parties', { mode: '랭크' }, { authScope: '7' }],
    )
    assert.deepEqual(
      communityPartyPostsScopedQueryKey({}, 'anonymous'),
      ['community', 'parties', { authScope: 'anonymous' }],
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
    assert.equal(response.data[0]?.close, '마감 시간 없음')
  })

  it('getPartyPosts는 마감 시간이 있으면 실제 마감 시각을 표시한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [
        {
          chatRoomId: 'party-recruitment',
          content: '마감 시간 표시 확인',
          currentMembers: 1,
          deadline: '2099-01-01T21:00:00',
          gameMode: 'RANKED_TFT',
          id: 10,
          maxMembers: 2,
          tags: [],
          title: '랭크 같이 해요',
        },
      ],
      success: true,
    })

    const response = await getPartyPosts()

    assert.equal(response.source, 'api')
    assert.equal(response.data[0]?.close, '1/1 21:00 마감')
    assert.equal(response.data[0]?.isDeadlineExpired, false)
  })

  it('getPartyPosts는 지난 마감 시간을 optimistic 상태 계산용으로 보존한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [
        {
          chatRoomId: 'party-recruitment',
          closed: true,
          content: '마감 시간 경과 확인',
          currentMembers: 2,
          deadline: '2020-01-01T21:00:00',
          gameMode: 'RANKED_TFT',
          id: 12,
          maxMembers: 2,
          tags: [],
          title: '마감된 랭크 파티',
        },
      ],
      success: true,
    })

    const response = await getPartyPosts()

    assert.equal(response.source, 'api')
    assert.equal(response.data[0]?.isDeadlineExpired, true)
  })

  it('getPartyPosts는 티어 조건을 태그에서 파생한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [
        {
          chatRoomId: 'party-recruitment',
          content: '마스터 이상만 구해요',
          currentMembers: 1,
          gameMode: 'RANKED_TFT',
          id: 11,
          maxMembers: 4,
          tags: ['마스터+', '음성 가능'],
          title: '랭크 파티',
        },
      ],
      success: true,
    })

    const response = await getPartyPosts()

    assert.equal(response.source, 'api')
    assert.equal(response.data[0]?.tier, '마스터+')
    assert.deepEqual(response.data[0]?.tags, ['마스터+', '음성 가능'])
  })

  it('getPartyPosts는 전체 조회일 때 모드 파라미터를 보내지 않는다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      data: [],
      success: true,
    })

    const response = await getPartyPosts()

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/community/parties')
    assert.equal(requestCalls[0]?.params, undefined)
    assert.equal(response.source, 'api')
    assert.deepEqual(response.data, [])
  })

  it('getPartyPosts는 실패 응답에서 더미 모집글 대신 빈 unavailable 결과를 반환한다', async () => {
    axiosInstance.defaults.adapter = createPartyAdapter({
      message: '파티 목록 조회 실패',
      success: false,
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
      title: '일반 같이 해요',
    })
    const requestBody = readRequestData(requestCalls[0])

    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/community/parties')
    assert.equal(requestBody.content, '편하게 연습하실 분')
    assert.equal(requestBody.gameMode, 'NORMAL_TFT')
    assert.equal(requestBody.maxMembers, 4)
    assert.equal('tier' in requestBody, false)
    assert.equal(response.id, '7')
    assert.equal(response.mode, '일반')
    assert.equal(response.capacity, '1/4')
    assert.equal(response.tier, '제한 없음')
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
          tags: ['마스터+', '랭크'],
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
          tags: ['마스터+', '랭크'],
          title: '마스터 듀오',
        }),
      /파티 모집글 등록 응답이 올바르지 않습니다/,
    )
  })

  it('createPartyPost는 활성 파티가 있으면 준비중인 기능 안내를 노출한다', async () => {
    axiosInstance.defaults.adapter = createRejectingPartyAdapter({
      message: '이미 참여 중인 파티가 있습니다.',
      success: false,
    })

    await assert.rejects(
      () =>
        createPartyPost({
          capacity: '1/2',
          deadline: '2026-06-16T21:00:00',
          description: '랭크 듀오 구합니다',
          mode: '랭크',
          tags: ['마스터+', '랭크'],
          title: '마스터 듀오',
        }),
      /기존 파티 종료 후 새 모집글을 작성하는 기능은 준비중입니다/,
    )
  })

  it('createPartyPost는 일반 실패 응답에는 기본 등록 실패 메시지를 사용한다', async () => {
    axiosInstance.defaults.adapter = createRejectingPartyAdapter({
      success: false,
    }, 500)

    await assert.rejects(
      () =>
        createPartyPost({
          capacity: '1/2',
          deadline: '2026-06-16T21:00:00',
          description: '랭크 듀오 구합니다',
          mode: '랭크',
          tags: ['마스터+', '랭크'],
          title: '마스터 듀오',
        }),
      /파티 모집글 등록 실패/,
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
