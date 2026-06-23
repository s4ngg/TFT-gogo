import assert from 'node:assert/strict'
import test from 'node:test'

import { normalizeGuideCatalog } from '../guideNormalizers'
import type { GuideCatalog } from '../guideTypes'

const fallbackCatalog: GuideCatalog = {
  augments: [],
  augmentPlans: [
    {
      key: 'reroll',
      label: '기본 플랜',
      stages: [],
    },
  ],
  champions: [],
  items: [],
  patchVersion: 'fallback',
  traits: [],
}

test('normalizeGuideCatalog reads entries and augment support data from catalog object payload', () => {
  const result = normalizeGuideCatalog({
    augmentPlans: [
      {
        key: 'fast8',
        label: '빠른 8레벨',
        stages: [
          {
            choice: '전투 증강',
            focus: '초반 전투력',
            stage: '2-1',
          },
        ],
      },
    ],
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
  assert.equal(result.traits[0].specialUnits?.[0]?.name, '소형 블랙홀')
  assert.equal(result.traits[0].name, '동물특공대')
  assert.equal(result.augmentPlans.length, 1)
  assert.equal(result.augmentPlans[0].label, '빠른 8레벨')
})

test('normalizeGuideCatalog keeps augment entries without imageUrl for fallback rendering', () => {
  const result = normalizeGuideCatalog({
    entries: [
      {
        dataJson: {
          description: 'Pick when the board needs tempo.',
          tags: ['tempo'],
        },
        guideType: 'AUGMENT',
        id: 2,
        imageUrl: '',
        name: 'Tempo Choice',
        patchVersion: '17.1',
        sortOrder: 0,
        summary: 'Pick when the board needs tempo.',
        targetKey: 'TFT17_TempoChoice',
      },
    ],
    patchVersion: '17.1',
  }, fallbackCatalog)

  assert.equal(result.augments.length, 1)
  assert.equal(result.augments[0].name, 'Tempo Choice')
  assert.equal(result.augments[0].imageUrl, '')
  assert.deepEqual(result.augments[0].tags, ['tempo'])

  const directPayload = normalizeGuideCatalog({
    augments: [
      {
        description: 'Fallback icon should render.',
        imageUrl: '',
        name: 'No Icon Augment',
        tags: [],
      },
    ],
  }, fallbackCatalog)

  assert.equal(directPayload.augments.length, 1)
  assert.equal(directPayload.augments[0].imageUrl, '')
})
