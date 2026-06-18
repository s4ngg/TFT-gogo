import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import type { PatchChange } from '../../../../api/patchNotes'
import {
  getPatchChangeDetailSummary,
  getPatchChangeTitle,
  getVisibleNewChangeTypes,
  groupPatchChangesByTitle,
  shouldShowPatchChangeValueLine,
} from '../patchChangeDisplay'

function patchChange(overrides: Partial<PatchChange>): PatchChange {
  return {
    after: '',
    before: '',
    category: '시너지',
    id: 1,
    impact: '중간',
    summary: '',
    tags: [],
    target: '',
    type: '조정',
    ...overrides,
  }
}

describe('patchChangeDisplay', () => {
  it('일반 대상명 대신 summary 전체가 들어온 변경사항은 콤마 앞 대상을 제목으로 사용한다', () => {
    const change = patchChange({
      summary: '중재자, 3회 기본 공격하면, 레오나 획득: 8/12% ⇒ 5/8%',
      target: '시너지',
    })

    assert.equal(getPatchChangeTitle(change), '중재자')
  })

  it('세부 설명에서는 반복되는 제목 prefix를 제거하고 화살표 표기를 통일한다', () => {
    const change = patchChange({
      summary: '중재자, 3회 기본 공격하면, 레오나 획득: 8/12% ⇒ 5/8%',
      target: '시너지',
    })

    assert.equal(
      getPatchChangeDetailSummary(change, '중재자'),
      '3회 기본 공격하면, 레오나 획득: 8/12% → 5/8%',
    )
  })

  it('target에 값 변화가 붙어 있어도 콜론 앞 설명만 제목으로 사용한다', () => {
    const change = patchChange({
      after: '공격력 290/435/730/1,250',
      before: '공격력 280/420/670/1,000',
      summary: '꽁! (나서스) 스킬 피해량: 공격력 280/420/670/1,000 ⇒ 공격력 290/435/730/1,250',
      target: '꽁! (나서스) 스킬 피해량: 공격력 280/420/670/1',
    })

    assert.equal(getPatchChangeTitle(change), '꽁! (나서스) 스킬 피해량')
    assert.equal(
      getPatchChangeDetailSummary(change, '꽁! (나서스) 스킬 피해량'),
      '공격력 280/420/670/1,000 → 공격력 290/435/730/1,250',
    )
    assert.equal(shouldShowPatchChangeValueLine(change), false)
  })

  it('summary에 before와 after가 이미 포함되면 별도 값 라인을 숨긴다', () => {
    const change = patchChange({
      after: '5/8%',
      before: '중재자, 3회 기본 공격하면, 레오나 획득: 8/12%',
      summary: '중재자, 3회 기본 공격하면, 레오나 획득: 8/12% ⇒ 5/8%',
    })

    assert.equal(shouldShowPatchChangeValueLine(change), false)
  })

  it('summary에 값 변화가 없으면 별도 값 라인을 표시한다', () => {
    const change = patchChange({
      after: '5/8%',
      before: '8/12%',
      summary: '중재자 레오나 획득량 조정',
    })

    assert.equal(shouldShowPatchChangeValueLine(change), true)
  })

  it('같은 대상명으로 파생된 변경사항을 하나의 그룹으로 묶는다', () => {
    const groups = groupPatchChangesByTitle([
      patchChange({
        id: 1,
        summary: '중재자, 3회 기본 공격하면, 레오나 획득: 8/12% ⇒ 5/8%',
        target: '시너지',
      }),
      patchChange({
        id: 2,
        summary: '중재자, 3회 기본 공격하면, 방어력 획득: 12/18 ⇒ 10/16',
        target: '시너지',
      }),
    ])

    assert.equal(groups.length, 1)
    assert.equal(groups[0].title, '중재자')
    assert.equal(groups[0].changes.length, 2)
  })

  it('신규 타입만 항목별 칩으로 표시한다', () => {
    assert.deepEqual(getVisibleNewChangeTypes([
      patchChange({ type: '조정' }),
    ]), [])

    assert.deepEqual(getVisibleNewChangeTypes([
      patchChange({ id: 1, type: '상향' }),
      patchChange({ id: 2, type: '하향' }),
    ]), [])

    assert.deepEqual(getVisibleNewChangeTypes([
      patchChange({ id: 1, type: '신규' }),
      patchChange({ id: 2, type: '하향' }),
    ]), ['신규'])
  })

  it('leading numeric trait tier is hidden and skill prefix is used as the title', () => {
    const change = patchChange({
      summary: '(10) 정령족 정령군주 스킬: 거대 강타 1차 피해량: 주문력 1,100 ⇒ 주문력 1,500, 광역 강타 피해량: 주문력 700 ⇒ 주문력 1,000, 기절 지속시간: 1.5초 ⇒ 3초, 기절 스킬 피해량: 주문력 150 ⇒ 주문력 400',
      target: '특성',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '정령족 정령군주 스킬:')
    assert.equal(
      getPatchChangeDetailSummary(change, title),
      '거대 강타 1차 피해량: 주문력 1,100 → 주문력 1,500, 광역 강타 피해량: 주문력 700 → 주문력 1,000, 기절 지속시간: 1.5초 → 3초, 기절 스킬 피해량: 주문력 150 → 주문력 400',
    )
  })
})
