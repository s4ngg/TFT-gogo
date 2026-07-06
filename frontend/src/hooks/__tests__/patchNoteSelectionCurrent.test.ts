import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { resolvePatchSelection } from '../patchNoteSelection'

describe('resolvePatchSelection current patch', () => {
  it('selects API current patch before the first list item', () => {
    assert.equal(resolvePatchSelection({
      currentPatchVersion: '17.4',
      hasUserSelectedPatch: false,
      isApiData: true,
      patchVersions: ['17.5', '17.4', '17.3'],
      selectedPatchVersion: '',
    }), '17.4')
  })
})
