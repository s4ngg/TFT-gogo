import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { resolveGuideImportPatchVersion } from '../guideImportVersion'

describe('resolveGuideImportPatchVersion', () => {
  it('uses the current patch version when latest is entered', () => {
    assert.equal(resolveGuideImportPatchVersion('latest', '17.6'), '17.6')
  })

  it('keeps latest when there is no current patch version', () => {
    assert.equal(resolveGuideImportPatchVersion('latest', null), 'latest')
  })

  it('keeps an explicit patch version', () => {
    assert.equal(resolveGuideImportPatchVersion(' 17.5 ', '17.6'), '17.5')
  })

  it('handles latest case-insensitively', () => {
    assert.equal(resolveGuideImportPatchVersion('LATEST', '17.6'), '17.6')
  })
})
