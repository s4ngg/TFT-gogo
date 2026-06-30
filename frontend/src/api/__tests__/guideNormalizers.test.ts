import assert from 'node:assert/strict'
import test from 'node:test'

import { normalizeGuideCatalog } from '../guideNormalizers'
import type { GuideCatalog } from '../guideTypes'

const fallbackCatalog: GuideCatalog = {
  augments: [],
  champions: [],
  items: [],
  patchVersion: 'fallback',
  traits: [],
}

test('normalizeGuideCatalog reads entries from catalog object payload', () => {
  const result = normalizeGuideCatalog({
    entries: [
      {
        dataJson: {
          champions: [
            {
              cost: 1,
              imageUrl: 'https://example.com/briar.png',
              name: '브라이어',
            },
          ],
          count: 2,
          levels: ['2'],
          summary: '아군이 공격력을 얻습니다.',
          specialUnits: [
            {
              imageUrl: 'https://example.com/blackhole.png',
              name: '소형 블랙홀',
              note: '특성 효과로 생성',
            },
          ],
          tierEffects: [],
          tips: [],
          tone: 'gold',
          variant: 'Huntress',
          type: '시너지',
        },
        guideType: 'TRAIT',
        id: 1,
        imageUrl: 'https://example.com/trait.png',
        name: '동물특공대',
        patchVersion: '17.1',
        sortOrder: 0,
        summary: '아군이 공격력을 얻습니다.',
        targetKey: 'TFT17_AnimalSquad',
      },
    ],
    patchVersion: '17.1',
  }, fallbackCatalog)

  assert.equal(result.patchVersion, '17.1')
  assert.equal(result.traits.length, 1)
  assert.equal(result.traits[0].targetKey, 'TFT17_AnimalSquad')
  assert.equal(result.traits[0].variant, 'Huntress')
  assert.equal(result.traits[0].specialUnits?.[0]?.name, '소형 블랙홀')
  assert.equal(result.traits[0].name, '동물특공대')
})
