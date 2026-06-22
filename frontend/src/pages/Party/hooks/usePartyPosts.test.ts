import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  getPartyCustomTagLimit,
  mergeTierIntoTags,
  normalizePartyDraftTags,
} from './usePartyPosts'

describe('party post tag helpers', () => {
  it('티어 조건을 하나만 앞에 두고 기존 티어 태그를 제거한다', () => {
    const tags = normalizePartyDraftTags('마스터+, 음성 가능, 다이아+, 순방 목표')

    assert.deepEqual(mergeTierIntoTags(tags, '플래티넘+'), ['플래티넘+', '음성 가능', '순방 목표'])
  })

  it('제한 없음이면 사용자가 입력한 티어 태그를 커스텀 태그로 저장하지 않는다', () => {
    const tags = normalizePartyDraftTags('마스터+, 음성 가능, 연습')

    assert.deepEqual(mergeTierIntoTags(tags, '제한 없음'), ['음성 가능', '연습'])
  })

  it('티어 조건 선택 여부에 따라 커스텀 태그 허용 개수를 계산한다', () => {
    assert.equal(getPartyCustomTagLimit('마스터+'), 3)
    assert.equal(getPartyCustomTagLimit('제한 없음'), 4)
  })
})
