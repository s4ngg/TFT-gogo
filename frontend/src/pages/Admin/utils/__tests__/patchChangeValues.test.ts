import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { shouldShowPatchChangeValues } from '../patchChangeValues'

describe('shouldShowPatchChangeValues', () => {
  it('hides the value line when summary already contains before and after values', () => {
    assert.equal(
      shouldShowPatchChangeValues({
        afterValue: '하급 챔피언 복제기 2개 + 5골드',
        beforeValue: '하급 챔피언 복제기 2개',
        summary: '하급 챔피언 복제기 2개 ⇒ 하급 챔피언 복제기 2개 + 5골드',
      }),
      false,
    )
  })

  it('shows the value line when summary only names the changed target', () => {
    assert.equal(
      shouldShowPatchChangeValues({
        afterValue: '공격력 290/435/730/1,250',
        beforeValue: '공격력 280/420/670/1,000',
        summary: '꽁! (나서스) 스킬 피해량',
      }),
      true,
    )
  })

  it('normalizes arrow characters and whitespace before comparing', () => {
    assert.equal(
      shouldShowPatchChangeValues({
        afterValue: '20%',
        beforeValue: '10%',
        summary: '공격 속도: 10% → 20%',
      }),
      false,
    )
  })

  it('hides the value line when both values are empty', () => {
    assert.equal(
      shouldShowPatchChangeValues({
        afterValue: null,
        beforeValue: null,
        summary: '버그 수정',
      }),
      false,
    )
  })
})
