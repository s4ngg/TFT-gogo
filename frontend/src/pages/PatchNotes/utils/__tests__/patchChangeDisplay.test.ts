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

  it('버그 수정 문구는 별도 배지 없이 제목만 자연스럽게 정리하고 반복 상세를 숨긴다', () => {
    const change = patchChange({
      summary: '태고족 우두머리가 전장에서 잘못된 위치에 소환되던 버그를 수정했습니다.',
      target: '버그 수정',
    })

    assert.equal(getPatchChangeTitle(change), '태고족 우두머리가 전장에서 잘못된 위치에 소환되던 문제 수정')
    assert.equal(getPatchChangeDetailSummary(change, '태고족 우두머리가 전장에서 잘못된 위치에 소환되던 문제 수정'), '')
    assert.equal(getPatchChangeStatusDisplay(change), undefined)
    assert.deepEqual(getVisiblePatchChangeStatuses([change]), [])
  })

  it('라벨만 있는 상세 문구는 패치노트 전체에서 설명 줄로 노출하지 않는다', () => {
    const change = patchChange({
      summary: '프리즘 이쉬탈이 전투 시작 시 2의 체력을 부여하지 않던 버그를 수정했습니다.',
      target: '버그 수정',
    })

    assert.equal(getPatchChangeTitle(change), '프리즘 이쉬탈이 전투 시작 시 2의 체력을 부여하지 않던 문제 수정')
    assert.deepEqual(getPatchChangeDetailLines(change, '프리즘 이쉬탈이 전투 시작 시 2의 체력을 부여하지 않던 문제 수정'), [])
  })

  it('유닛 단계 같은 generic target은 제목을 잘라 쓰지 않고 summary의 실제 변경 대상을 사용한다', () => {
    const change = patchChange({
      summary: '베이가 주문력 스킬 피해량: 310/465/700/1,190 ⇒ 330/495/750/1,200',
      target: '유닛: 1단계',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '베이가 주문력 스킬 피해량')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '310 / 465 / 700 / 1,190',
      '→ 330 / 495 / 750 / 1,200',
    ])
  })

  it('괄호 안 쉼표는 제목 구분자로 보지 않아 문장이 중간에 잘리지 않는다', () => {
    const change = patchChange({
      summary: '2단계 아이템 전투 토끼 석궁은 이제 기본 공격이 여러 번 적중하는 경우에도 중첩이 1회만 쌓입니다. (자야, 그레이브즈)',
      target: '특성',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '2단계 아이템 전투 토끼 석궁은 이제 기본 공격이 여러 번 적중하는 경우에도 중첩이 1회만 쌓입니다. (자야, 그레이브즈)')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [])
  })

  it('일반 문장 쉼표와 숫자 쉼표는 제목 구분자로 보지 않는다', () => {
    const change = patchChange({
      summary: '킨드레드 [업데이트] 이제 스킬이 아군의 사망을 방지하지 않습니다. 이제 영역 지속시간 동안 아군에게 주문력의 350/750/10,000%에 해당하는 보호막을 씌웁니다.',
      target: '유닛',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '킨드레드 [업데이트] 이제 스킬이 아군의 사망을 방지하지 않습니다.')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '이제 영역 지속시간 동안 아군에게 주문력의 350/750/10,000%에 해당하는 보호막을 씌웁니다.',
    ])
  })

  it('버그 수정 뒤에 수치 변경이 이어지는 항목은 제목과 수치 상세를 분리한다', () => {
    const change = patchChange({
      summary: '탈론의 스킬 피해에 모든 피해 흡혈이 적용되지 않던 버그를 수정했습니다. 공격 속도: 0.8 ⇒ 0.75, 스킬 피해량: 460/690/1,090 ⇒ 430/645/1,000',
      target: '유닛',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '탈론의 스킬 피해에 모든 피해 흡혈이 적용되지 않던 문제 수정')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '공격 속도: 0.8 → 0.75',
      '스킬 피해량: 460/690/1,090 → 430/645/1,000',
    ])
  })

  it('버그 수정 prefix가 붙은 항목도 prefix를 제목에 반복하지 않는다', () => {
    const change = patchChange({
      summary: '버그 수정: 일부 상황에서 니달리가 의도치 않게 적의 대상을 바꾸게 하던 버그를 수정했습니다.',
      target: '1월 27일',
    })

    assert.equal(getPatchChangeTitle(change), '일부 상황에서 니달리가 의도치 않게 적의 대상을 바꾸게 하던 문제 수정')
  })

  it('long slash-separated value lists are condensed to only changed values', () => {
    const change = patchChange({
      summary: 'Ixtal reward thresholds: 25/35/50/80/120/150/200/250/300/350/400/450/500/550/625/700/800/900/1,000 → 25/35/50/80/120/150/200/250/300/350/400/450/500/550/625/710/830/920/1,000',
      target: 'Pengu Party',
    })

    assert.deepEqual(getPatchChangeDetailLines(change, 'Pengu Party'), [
      'Ixtal reward thresholds:',
      '700 / 800 / 900',
      '→ 710 / 830 / 920',
    ])
  })

  it('short slash-separated value changes stay as the original readable line', () => {
    const change = patchChange({
      summary: 'Trait bonus: 8/12% → 5/8%',
      target: 'Arbiter',
    })

    assert.deepEqual(getPatchChangeDetailLines(change, 'Arbiter'), [
      'Trait bonus: 8/12% → 5/8%',
    ])
  })

  it('긴 설명형 문장은 첫 핵심 문장을 제목으로 쓰고 나머지 문장을 상세로 분리한다', () => {
    const change = patchChange({
      summary: '신 짜오는 현재 K.O. 콜로세움에 건강하게 살아있습니다. 다만 행동이 약간 이상해졌고, 다르킨과 관련해 소리를 지르고 다닌다고 하네요. 최근 황혼의 종말 영상을 보셨다면 농담이 이해되실 겁니다. 멋진 영상이죠.',
      target: '유닛',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '신 짜오는 현재 K.O. 콜로세움에 건강하게 살아있습니다.')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '다만 행동이 약간 이상해졌고, 다르킨과 관련해 소리를 지르고 다닌다고 하네요. 최근 황혼의 종말 영상을 보셨다면 농담이 이해되실 겁니다. 멋진 영상이죠.',
    ])
  })

  it('긴 조건형 변경 문장은 앞 조건절을 줄이고 원문을 상세로 보존한다', () => {
    const change = patchChange({
      summary: '챔피언이 치명적인 칼날 증강과 함께 죽음의 검을 여러 개 장착한 경우, 처치 관여 시 공격력 중첩을 정상적으로 여러 번 획득합니다. (이전에는 라운드 시작 시 자동으로 수정되었습니다.)',
      target: '버그 수정',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '처치 관여 시 공격력 중첩을 정상적으로 여러 번 획득합니다.')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '챔피언이 치명적인 칼날 증강과 함께 죽음의 검을 여러 개 장착한 경우, 처치 관여 시 공격력 중첩을 정상적으로 여러 번 획득합니다. (이전에는 라운드 시작 시 자동으로 수정되었습니다.)',
    ])
  })

  it('긴 버그 수정 조건문은 핵심 문제 제목으로 줄이고 원문을 상세로 보존한다', () => {
    const change = patchChange({
      summary: '상징을 통해 N.O.V.A. 유닛 6명을 전장에 배치한 상태에서 N.O.V.A. 상징을 제거한 다음 이를 다른 유닛에 장착하지 않은 경우, 원래 상징을 갖고 있던 유닛에 그 효과가 계속해서 적용되던 버그를 수정했습니다.',
      target: '버그 수정',
    })

    const title = getPatchChangeTitle(change)

    assert.equal(title, '원래 상징을 갖고 있던 유닛에 그 효과가 계속해서 적용되던 문제 수정')
    assert.deepEqual(getPatchChangeDetailLines(change, title), [
      '상징을 통해 N.O.V.A. 유닛 6명을 전장에 배치한 상태에서 N.O.V.A. 상징을 제거한 다음 이를 다른 유닛에 장착하지 않은 경우, 원래 상징을 갖고 있던 유닛에 그 효과가 계속해서 적용되던 버그를 수정했습니다.',
    ])
  })
})
