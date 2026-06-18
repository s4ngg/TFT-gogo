import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import type { PatchChange } from '../../../../api/patchNotes'
import {
  getPatchChangeDetailLines,
  getPatchChangeDetailSummary,
  getPatchChangeStatusDisplay,
  getPatchChangeTitle,
  getVisiblePatchChangeStatuses,
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

  it('thousand separators are not treated as title delimiters', () => {
    const change = patchChange({
      summary: 'Attack 1,000 ⇒ 1,200',
      target: '',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, 'Attack 1,000 → 1,200')
    assert.equal(getPatchChangeDetailSummary(change, title), '')
  })

  it('증강체 활성화 상태 문구는 이름과 상태 배지 정보로 분리한다', () => {
    const change = patchChange({
      category: '증강체',
      summary: '번들 현상금 I이 다시 활성화됩니다.',
      target: '증강',
    })

    assert.equal(getPatchChangeTitle(change), '번들 현상금 I')
    assert.equal(getPatchChangeDetailSummary(change, '번들 현상금 I'), '')
    assert.deepEqual(getPatchChangeStatusDisplay(change), {
      label: '복귀',
      title: '번들 현상금 I',
      tone: 'enabled',
    })
  })

  it('증강체 비활성화 상태 문구도 같은 방식으로 분리한다', () => {
    const change = patchChange({
      category: '증강체',
      summary: '황혼의 시험 II가 비활성화됩니다.',
      target: '증강',
    })

    assert.equal(getPatchChangeTitle(change), '황혼의 시험 II')
    assert.equal(getPatchChangeDetailSummary(change, '황혼의 시험 II'), '')
    assert.deepEqual(getVisiblePatchChangeStatuses([change]), [
      {
        label: '제외',
        title: '황혼의 시험 II',
        tone: 'disabled',
      },
    ])
  })

  it('제목 앞에 붙은 쉼표, 따옴표, 숫자 티어 표기는 숨긴다', () => {
    assert.equal(
      getPatchChangeTitle(patchChange({
        summary: "', (5) 도전자 공격 속도: 100/125% ⇒ 90/115%",
        target: '특성',
      })),
      '도전자 공격 속도',
    )

    assert.equal(
      getPatchChangeTitle(patchChange({
        summary: '‘이전 준비 단계 동안 새로고침한 경우 획득하는 보호막의 최대 체력 계수: 40/60% ⇒ 44/66%',
        target: '시너지',
      })),
      '이전 준비 단계 동안 새로고침한 경우 획득하는 보호막의 최대 체력 계수',
    )
  })

  it('추가와 삭제 상태 문구는 사용자용 짧은 배지 라벨로 표시한다', () => {
    assert.deepEqual(
      getPatchChangeStatusDisplay(patchChange({
        summary: '새로운 증강이 추가됩니다.',
        target: '증강',
      })),
      {
        label: '신규',
        title: '새로운 증강',
        tone: 'added',
      },
    )

    assert.deepEqual(
      getPatchChangeStatusDisplay(patchChange({
        summary: '오래된 증강이 삭제됩니다.',
        target: '증강',
      })),
      {
        label: '제거',
        title: '오래된 증강',
        tone: 'removed',
      },
    )
  })

  it('여러 값 변화가 이어지는 상세 문장은 항목별 줄로 나눈다', () => {
    const change = patchChange({
      summary: '혼돈의 부름: 찬란한 행운의 아이템 상자 + 15골드 ⇒ 찬란한 행운의 아이템 상자 + 8골드, 58골드 ⇒ 52골드, 경험치 64 ⇒ 경험치 58, 새로고침: 40 ⇒ 36',
      target: '증강',
    })

    assert.deepEqual(getPatchChangeDetailLines(change, '혼돈의 부름'), [
      '찬란한 행운의 아이템 상자 + 15골드 → 찬란한 행운의 아이템 상자 + 8골드',
      '58골드 → 52골드',
      '경험치 64 → 경험치 58',
      '새로고침: 40 → 36',
    ])
  })

  it('상태 문구 제목에서는 설명용 부사와 반복 대상명을 일반 규칙으로 제거한다', () => {
    const status = getPatchChangeStatusDisplay(patchChange({
      summary: '펑구의 파티: 조우자 없음 조우자가 정상적으로 비활성화됩니다.',
      target: '시스템',
    }))

    assert.deepEqual(status, {
      label: '제외',
      title: '펑구의 파티: 조우자 없음',
      tone: 'disabled',
    })

    assert.deepEqual(
      getPatchChangeStatusDisplay(patchChange({
        summary: '오른의 유물 아이템이 제대로 삭제됩니다.',
        target: '아이템',
      })),
      {
        label: '제거',
        title: '오른의 유물 아이템',
        tone: 'removed',
      },
    )

    assert.deepEqual(
      getPatchChangeStatusDisplay(patchChange({
        summary: '프리즘 보관함 보관함이 올바르게 추가됩니다.',
        target: '시스템',
      })),
      {
        label: '신규',
        title: '프리즘 보관함',
        tone: 'added',
      },
    )

    assert.deepEqual(
      getPatchChangeStatusDisplay(patchChange({
        summary: '전략가 체력 체력이 정삭적으로 비활성화됩니다.',
        target: '시스템',
      })),
      {
        label: '제외',
        title: '전략가 체력',
        tone: 'disabled',
      },
    )
  })
})
