import assert from 'node:assert/strict'
import test from 'node:test'

import { formatGuideRankMetric, isDisplayableGuideTag, sanitizeGuideText } from '../guideText'

test('isDisplayableGuideTag filters internal CDragon hash tags', () => {
  assert.equal(isDisplayableGuideTag('{cf1fd3af}'), false)
  assert.equal(isDisplayableGuideTag('아이템'), true)
})

test('formatGuideRankMetric avoids hash prefix for empty metric placeholders', () => {
  assert.equal(formatGuideRankMetric('-'), '-')
  assert.equal(formatGuideRankMetric('3.21'), '#3.21')
})

test('sanitizeGuideText removes CDragon display tokens while preserving readable text', () => {
  const text = '아군이 <b>공격력</b>을 얻습니다. %i:scaleAS% 공격 속도가 % 증가합니다. ()'

  assert.equal(sanitizeGuideText(text), '아군이 공격력을 얻습니다. 공격 속도가 증가합니다.')
})

test('sanitizeGuideText keeps real percent values', () => {
  const text = '피해량이 10 % 증가하고 %i:scaleHealth% 체력이 % 증가합니다.'

  assert.equal(sanitizeGuideText(text), '피해량이 10% 증가하고 체력이 증가합니다.')
})
