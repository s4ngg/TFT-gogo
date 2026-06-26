import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import {
  createAdminPatchChange,
  createAdminPatchNote,
  fetchAdminPatchChanges,
  fetchAdminPatchNotes,
  importAdminPatchNoteFromRiot,
  isAdminAuthFailure,
  setAccessToken,
  clearAccessToken,
  updateAdminPatchChange,
  type AdminPatchChangePayload,
  type AdminPatchNoteImportRequest,
  type AdminPatchNotePayload,
} from '../adminApi'
import axiosInstance from '../axiosInstance'

interface RequestCall {
  data?: unknown
  method?: string
  params?: unknown
  timeout?: unknown
  token?: unknown
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function parseRequestData(data: unknown): unknown {
  if (typeof data !== 'string') return data
  return JSON.parse(data) as unknown
}

function readAuthToken(config: InternalAxiosRequestConfig): unknown {
  const headers = config.headers
  if (typeof headers?.get === 'function') return headers.get('Authorization')
  return (headers as Record<string, unknown> | undefined)?.['Authorization']
}

function createAdapter(responseData: unknown): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: parseRequestData(config.data),
      method: config.method,
      params: config.params,
      timeout: config.timeout,
      token: readAuthToken(config),
      url: config.url,
    })

    return {
      config,
      data: {
        data: responseData,
        success: true,
      },
      headers: {},
      status: 200,
      statusText: 'OK',
    }
  }
}

function createErrorAdapter(status: number): AxiosAdapter {
  return async (): Promise<AxiosResponse> => {
    throw {
      message: 'Request failed',
      response: { status },
    }
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  requestCalls.length = 0
  clearAccessToken()
})

describe('admin patch note api', () => {
  it('관리자 패치노트 목록을 admin 엔드포인트에서 조회한다', async () => {
    axiosInstance.defaults.adapter = createAdapter([
      {
        changeCount: 2,
        description: 'description',
        focus: 'focus',
        highlights: ['highlight'],
        id: 1,
        imageUrl: null,
        isCurrent: true,
        publishedAt: '2026-06-12T10:00:00',
        summary: 'summary',
        title: '17.3 패치노트',
        version: '17.3',
      },
    ])
    setAccessToken('test-access-token')

    const response = await fetchAdminPatchNotes()

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/admin/patch-notes')
    assert.equal(requestCalls[0]?.token, 'Bearer test-access-token')
    assert.equal(response[0]?.version, '17.3')
  })

  it('admin patch note api error preserves auth failure status', async () => {
    axiosInstance.defaults.adapter = createErrorAdapter(403)

    await assert.rejects(
      async () => {
        await fetchAdminPatchNotes()
      },
      (error) => {
        assert.equal(error instanceof Error, true)
        assert.equal(isAdminAuthFailure(error), true)
        return true
      },
    )
  })

  it('패치노트 생성 요청을 백엔드 관리자 계약에 맞춰 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 1, version: '17.3' })
    setAccessToken('test-access-token')
    const payload: AdminPatchNotePayload = {
      current: true,
      description: null,
      focus: '챔피언 조정',
      highlights: ['징크스 상향'],
      imageUrl: null,
      publishedAt: '2026-06-12T10:00',
      summary: '핵심 요약',
      title: '17.3 패치노트',
      version: '17.3',
    }

    await createAdminPatchNote(payload)

    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/admin/patch-notes')
    assert.deepEqual(requestCalls[0]?.data, payload)
  })

  it('Riot patch note import uses the admin import endpoint', async () => {
    axiosInstance.defaults.adapter = createAdapter({
      createdChanges: 3,
      parserWarnings: [],
      patchNoteCreated: true,
      patchNoteId: 7,
      patchNoteSkipped: false,
      patchNoteUpdated: false,
      skippedChanges: 0,
      sourceUrl: 'https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/test',
      updatedChanges: 0,
      version: '17.5',
    })
    setAccessToken('test-access-token')
    const payload: AdminPatchNoteImportRequest = {
      current: true,
      locale: 'ko-kr',
      sourceUrl: null,
      version: null,
    }

    const response = await importAdminPatchNoteFromRiot(payload)

    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/admin/patch-notes/import/riot')
    assert.equal(requestCalls[0]?.token, 'Bearer test-access-token')
    assert.equal(requestCalls[0]?.timeout, 120_000)
    assert.deepEqual(requestCalls[0]?.data, payload)
    assert.equal(response.patchNoteId, 7)
  })

  it('선택 패치의 변경사항은 관리자 변경사항 조회 API를 사용한다', async () => {
    axiosInstance.defaults.adapter = createAdapter([])
    setAccessToken('test-access-token')

    await fetchAdminPatchChanges(1)

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/admin/patch-notes/1/changes')
    assert.equal(requestCalls[0]?.token, 'Bearer test-access-token')
  })

  it('패치 변경사항 수정 요청을 patch-note-changes 엔드포인트로 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 10, targetName: '징크스' })
    setAccessToken('test-access-token')
    const payload: AdminPatchChangePayload = {
      afterValue: '240',
      beforeValue: '220',
      category: 'CHAMPION',
      imageUrl: null,
      impact: 'HIGH',
      patchNoteId: 1,
      sortOrder: 0,
      summary: '스킬 피해량 증가',
      tags: ['챔피언', '상향'],
      targetKey: 'TFT17_Jinx',
      targetName: '징크스',
      type: 'BUFF',
    }

    await updateAdminPatchChange(10, payload)

    assert.equal(requestCalls[0]?.method, 'patch')
    assert.equal(requestCalls[0]?.url, '/admin/patch-note-changes/10')
    assert.deepEqual(requestCalls[0]?.data, payload)
    assert.equal(requestCalls[0]?.token, 'Bearer test-access-token')
  })

  it('패치 변경사항 생성 요청을 patch-note-changes 엔드포인트로 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 11, targetName: '무한의 대검' })
    setAccessToken('test-access-token')
    const payload: AdminPatchChangePayload = {
      afterValue: null,
      beforeValue: null,
      category: 'ITEM',
      imageUrl: null,
      impact: 'MEDIUM',
      patchNoteId: 1,
      sortOrder: 1,
      summary: '아이템 효과 조정',
      tags: [],
      targetKey: 'TFT_Item_InfinityEdge',
      targetName: '무한의 대검',
      type: 'ADJUST',
    }

    await createAdminPatchChange(payload)

    assert.equal(requestCalls[0]?.method, 'post')
    assert.equal(requestCalls[0]?.url, '/admin/patch-note-changes')
    assert.deepEqual(requestCalls[0]?.data, payload)
    assert.equal(requestCalls[0]?.token, 'Bearer test-access-token')
  })
})
