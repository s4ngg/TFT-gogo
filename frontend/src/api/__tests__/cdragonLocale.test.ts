import assert from 'node:assert/strict'
import { afterEach, describe, it } from 'node:test'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import axiosInstance from '../axiosInstance'
import { fetchTFTLocale } from '../cdragonLocale'

interface RequestCall {
  method?: string
  timeout?: unknown
  url?: string
}

const originalAdapter = axiosInstance.defaults.adapter
const requestCalls: RequestCall[] = []

function createCDragonLocaleAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    requestCalls.push({
      method: config.method,
      timeout: config.timeout,
      url: config.url,
    })

    return {
      config,
      data: {
        data: {
          items: [
            {
              apiName: 'TFT_Item_InfinityEdge',
              name: 'Infinity Edge',
            },
          ],
          sets: {
            17: {
              augments: [
                {
                  apiName: 'TFT17_Augment_TestAugment',
                  name: 'Test Augment',
                },
              ],
              champions: [
                {
                  apiName: 'TFT17_Jinx',
                  cost: 2,
                  name: 'Jinx',
                  traits: ['TFT17_Challenger'],
                },
              ],
              traits: [
                {
                  apiName: 'TFT17_Challenger',
                  effects: [{ minUnits: 2, style: 3 }],
                  name: 'Challenger',
                },
              ],
            },
          },
        },
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

describe('cdragonLocale', () => {
  it('calls the backend CDragon proxy to avoid browser CORS', async () => {
    axiosInstance.defaults.adapter = createCDragonLocaleAdapter()

    const locale = await fetchTFTLocale()

    assert.equal(requestCalls[0]?.method, 'get')
    assert.equal(requestCalls[0]?.url, '/cdragon/tft/ko-kr')
    assert.equal(requestCalls[0]?.timeout, 60_000)
    assert.equal(locale.champByApiName.get('tft17_jinx'), 'Jinx')
    assert.equal(locale.traitBySuffix.get('challenger'), 'Challenger')
    assert.equal(locale.itemByApiName.get('tft_item_infinityedge'), 'Infinity Edge')
    assert.equal(locale.augmentBySuffix.get('testaugment'), 'Test Augment')
  })
})
