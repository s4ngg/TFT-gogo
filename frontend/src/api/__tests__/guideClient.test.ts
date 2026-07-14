import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse } from 'axios'

import axiosInstance from '../axiosInstance'
import { getGuidePatchVersion, getGuideTabItems } from '../guideClient'
import type { GuideCatalog } from '../guideTypes'

const originalAdapter = axiosInstance.defaults.adapter

const fallbackCatalog: GuideCatalog = {
  augments: [{
    description: '17.0 fallback augment',
    imageUrl: '/fallback.png',
    name: '17.0 전용 증강체',
    tags: [],
  }],
  champions: [],
  items: [],
  patchVersion: '17.0',
  traits: [],
}

function toRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    throw new Error('expected object payload')
  }

  return value as Record<string, unknown>
}

function createGuideResponse(config: Parameters<AxiosAdapter>[0]): AxiosResponse {
  return {
    config,
    data: {
      data: {
        items: [],
        page: 1,
        pageSize: 6,
        totalItems: 0,
        totalPages: 1,
      },
      success: true,
    },
    headers: {},
    status: 200,
    statusText: 'OK',
  }
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
})

describe('getGuidePatchVersion', () => {
  it('공백 패치 버전 응답은 fallback 버전으로 통일한다', async () => {
    const adapter: AxiosAdapter = async (config): Promise<AxiosResponse> => ({
      config,
      data: {
        data: { patchVersion: '   ' },
        success: true,
      },
      headers: {},
      status: 200,
      statusText: 'OK',
    })
    axiosInstance.defaults.adapter = adapter

    const result = await getGuidePatchVersion(fallbackCatalog.patchVersion)

    assert.equal(result.patchVersion, '17.0')
    assert.equal(result.source, 'fallback')
  })
})

describe('getGuideTabItems', () => {
  it('탭 조회 요청에 기준 패치 버전을 전달한다', async () => {
    let capturedParams: unknown

    const adapter: AxiosAdapter = async (config): Promise<AxiosResponse> => {
      capturedParams = config.params

      return createGuideResponse(config)
    }
    axiosInstance.defaults.adapter = adapter

    await getGuideTabItems({
      page: 1,
      pageSize: 6,
      patchVersion: '17.6',
      query: '',
      tab: 'traits',
    }, fallbackCatalog)

    const params = toRecord(capturedParams)

    assert.equal(params.patchVersion, '17.6')
  })

  it('빈 패치 버전은 탭 조회 요청에서 제외한다', async () => {
    let capturedParams: unknown

    const adapter: AxiosAdapter = async (config): Promise<AxiosResponse> => {
      capturedParams = config.params

      return createGuideResponse(config)
    }
    axiosInstance.defaults.adapter = adapter

    await getGuideTabItems({
      page: 1,
      pageSize: 6,
      patchVersion: '',
      query: '',
      tab: 'traits',
    }, fallbackCatalog)

    const params = toRecord(capturedParams)

    assert.equal(params.patchVersion, undefined)
  })

  it('기준 버전 조회 성공 후 탭 조회가 실패해도 다른 버전 fallback을 섞지 않는다', async () => {
    const adapter: AxiosAdapter = async (config): Promise<AxiosResponse> => {
      if (config.url !== '/guide/patch-version') {
        throw new Error('guide tab request failed')
      }

      return {
        config,
        data: {
          data: { patchVersion: '17.1' },
          success: true,
        },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    }
    axiosInstance.defaults.adapter = adapter

    const patchVersionResult = await getGuidePatchVersion(fallbackCatalog.patchVersion)
    const tabResult = await getGuideTabItems({
      page: 1,
      pageSize: 6,
      patchVersion: patchVersionResult.patchVersion,
      query: '',
      tab: 'augments',
    }, fallbackCatalog)

    assert.equal(patchVersionResult.source, 'api')
    assert.equal(tabResult.patchVersion, '17.1')
    assert.equal(tabResult.source, 'unavailable')
    assert.deepEqual(tabResult.data.items, [])
  })

  it('탭 조회가 실패하면 요청 버전과 같은 fallback만 사용한다', async () => {
    axiosInstance.defaults.adapter = async () => {
      throw new Error('guide tab request failed')
    }

    const result = await getGuideTabItems({
      page: 1,
      pageSize: 6,
      patchVersion: '17.0',
      query: '',
      tab: 'augments',
    }, fallbackCatalog)

    assert.equal(result.patchVersion, '17.0')
    assert.equal(result.source, 'fallback')
    assert.deepEqual(result.data.items.map((augment) => augment.name), ['17.0 전용 증강체'])
  })
})
