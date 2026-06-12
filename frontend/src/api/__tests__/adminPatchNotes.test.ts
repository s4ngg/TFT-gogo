import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import {
  createAdminPatchChange,
  createAdminPatchNote,
  fetchAdminPatchChanges,
  fetchAdminPatchNotes,
  setAdminToken,
  updateAdminPatchChange,
  type AdminPatchChangePayload,
  type AdminPatchNotePayload,
} from '../adminApi'
import axiosInstance from '../axiosInstance'

interface RequestCall {
  data?: unknown
  method?: string
  params?: unknown
  token?: unknown
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []
const storage = new Map<string, string>()

Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: {
    clear: () => storage.clear(),
    getItem: (key: string) => storage.get(key) ?? null,
    removeItem: (key: string) => storage.delete(key),
    setItem: (key: string, value: string) => storage.set(key, value),
  },
})

function parseRequestData(data: unknown): unknown {
  if (typeof data !== 'string') return data
  return JSON.parse(data) as unknown
}

function readAdminToken(config: InternalAxiosRequestConfig): unknown {
  const headers = config.headers
  if (typeof headers?.get === 'function') return headers.get('X-Admin-Token')
  return (headers as Record<string, unknown> | undefined)?.['X-Admin-Token']
}

function createAdapter(responseData: unknown): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      data: parseRequestData(config.data),
      method: config.method,
      params: config.params,
      token: readAdminToken(config),
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

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
  requestCalls.length = 0
  storage.clear()
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
    setAdminToken('admin-token')

    const response = await fetchAdminPatchNotes()

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/admin/patch-notes')
    assert.equal(requestCalls[0]?.token, 'admin-token')
    assert.equal(response[0]?.version, '17.3')
  })

  it('패치노트 생성 요청을 백엔드 관리자 계약에 맞춰 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 1, version: '17.3' })
    setAdminToken('admin-token')
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

  it('선택 패치의 변경사항은 공개 변경사항 조회 API를 재사용한다', async () => {
    axiosInstance.defaults.adapter = createAdapter({
      items: [],
      page: 1,
      pageSize: 100,
      totalItems: 0,
      totalPages: 1,
    })

    await fetchAdminPatchChanges('17.3', 1, 100)

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/patch-notes/17.3/changes')
    assert.deepEqual(requestCalls[0]?.params, { page: 1, pageSize: 100 })
  })

  it('패치 변경사항 수정 요청을 patch-note-changes 엔드포인트로 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 10, targetName: '징크스' })
    setAdminToken('admin-token')
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
    assert.equal(requestCalls[0]?.token, 'admin-token')
  })

  it('패치 변경사항 생성 요청을 patch-note-changes 엔드포인트로 보낸다', async () => {
    axiosInstance.defaults.adapter = createAdapter({ id: 11, targetName: '무한의 대검' })
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
  })
})
