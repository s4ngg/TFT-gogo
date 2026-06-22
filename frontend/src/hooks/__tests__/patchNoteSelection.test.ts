import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { resolvePatchSelection } from '../patchNoteSelection'

describe('resolvePatchSelection', () => {
  it('선택된 버전이 아직 없으면 API 최신 패치를 선택한다', () => {
    assert.equal(resolvePatchSelection({
      hasUserSelectedPatch: false,
      isApiData: true,
      patchVersions: ['17.5', '17.4', '17.3'],
      selectedPatchVersion: '',
    }), '17.5')
  })

  it('API 데이터가 처음 도착하면 fallback 기본 선택 대신 최신 패치를 선택한다', () => {
    assert.equal(resolvePatchSelection({
      hasUserSelectedPatch: false,
      isApiData: true,
      patchVersions: ['17.5', '17.4', '17.3'],
      selectedPatchVersion: '17.3',
    }), '17.5')
  })

  it('사용자가 직접 과거 패치를 선택한 경우 API refetch가 와도 선택을 유지한다', () => {
    assert.equal(resolvePatchSelection({
      hasUserSelectedPatch: true,
      isApiData: true,
      patchVersions: ['17.5', '17.4', '17.3'],
      selectedPatchVersion: '17.3',
    }), '17.3')
  })

  it('선택된 버전이 목록에서 사라지면 최신 패치로 보정한다', () => {
    assert.equal(resolvePatchSelection({
      hasUserSelectedPatch: true,
      isApiData: true,
      patchVersions: ['17.5', '17.4'],
      selectedPatchVersion: '17.3',
    }), '17.5')
  })

  it('패치 목록이 비어 있으면 현재 선택값을 유지한다', () => {
    assert.equal(resolvePatchSelection({
      hasUserSelectedPatch: false,
      isApiData: false,
      patchVersions: [],
      selectedPatchVersion: '17.3',
    }), '17.3')
  })
})
