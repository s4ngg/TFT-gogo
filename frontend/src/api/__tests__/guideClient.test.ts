import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse } from 'axios'

import axiosInstance from '../axiosInstance'
import { getGuideTabItems } from '../guideClient'
import type { GuideCatalog } from '../guideTypes'

const originalAdapter = axiosInstance.defaults.adapter

const fallbackCatalog: GuideCatalog = {
  augments: [],
  champions: [],
  items: [],
  patchVersion: 'fallback',
  traits: [],
}

function toRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    throw new Error('expected object payload')
  }

  return value as Record<string, unknown>
}

afterEach(() => {
  axiosInstance.defaults.adapter = originalAdapter
})

describe('getGuideTabItems', () => {
  it('탭 조회 요청에 기준 패치 버전을 전달한다', async () => {
    let capturedParams: unknown

    const adapter: AxiosAdapter = async (config): Promise<AxiosResponse> => {
      capturedParams = config.params

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
})
