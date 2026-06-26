import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import { getEstimatedGuideImportProgress } from '../guideImportProgress'

describe('getEstimatedGuideImportProgress', () => {
  it('starts with a visible progress value', () => {
    assert.equal(getEstimatedGuideImportProgress(0), 8)
  })

  it('increases with elapsed seconds', () => {
    assert.equal(getEstimatedGuideImportProgress(5), 28)
  })

  it('caps estimated progress before completion', () => {
    assert.equal(getEstimatedGuideImportProgress(60), 95)
  })
})
