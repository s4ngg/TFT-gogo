import { useState, type FormEvent } from 'react'
import {
  importGuideCdragonData,
  type GuideCdragonImportRequest,
  type GuideImportResponse,
} from '../../api/adminApi'
import styles from './Admin.module.css'

interface GuideImportFormState {
  includeAugments: boolean
  includeChampions: boolean
  includeItems: boolean
  includeTraits: boolean
  mutator: string
  patchVersion: string
  setNumber: string
}

const DEFAULT_GUIDE_IMPORT_FORM: GuideImportFormState = {
  includeAugments: true,
  includeChampions: true,
  includeItems: true,
  includeTraits: true,
  mutator: 'TFTSet17',
  patchVersion: 'latest',
  setNumber: '17',
}

function AdminGuides() {
  const [form, setForm] = useState<GuideImportFormState>(DEFAULT_GUIDE_IMPORT_FORM)
  const [result, setResult] = useState<GuideImportResponse | null>(null)
  const [error, setError] = useState('')
  const [importing, setImporting] = useState(false)

  function patch<K extends keyof GuideImportFormState>(key: K, value: GuideImportFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function buildPayload(): GuideCdragonImportRequest | null {
    const patchVersion = form.patchVersion.trim()
    const setNumber = Number(form.setNumber)

    if (!patchVersion) {
      setError('패치 버전을 입력하세요.')
      return null
    }

    if (!Number.isInteger(setNumber) || setNumber < 1) {
      setError('세트 번호는 1 이상 정수여야 합니다.')
      return null
    }

    if (!form.includeChampions && !form.includeTraits && !form.includeItems && !form.includeAugments) {
      setError('챔피언, 특성, 아이템, 증강체 중 하나는 선택해야 합니다.')
      return null
    }

    return {
      includeAugments: form.includeAugments,
      includeChampions: form.includeChampions,
      includeItems: form.includeItems,
      includeTraits: form.includeTraits,
      mutator: form.mutator.trim() || null,
      patchVersion,
      setNumber,
    }
  }

  async function handleImport(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()

    const payload = buildPayload()
    if (!payload) return

    setImporting(true)
    setError('')
    try {
      const response = await importGuideCdragonData(payload)
      setResult(response)
    } catch {
      setError('가져오기에 실패했습니다. 토큰, 패치 버전, 세트 정보를 확인하세요.')
    } finally {
      setImporting(false)
    }
  }

  const importedCount = result?.importedCount ?? 0

  return (
    <div className={styles.page}>
      <div className={styles.guideImportPanel}>
        <div className={styles.toolbar}>
          <h1 className={styles.title}>게임가이드 Import</h1>
        </div>

        <form className={styles.guideImportForm} onSubmit={handleImport}>
          <div className={styles.guideImportGrid}>
            <label className={styles.guideImportField}>
              <span className={styles.guideImportLabel}>패치 버전</span>
              <input
                className={styles.guideImportInput}
                value={form.patchVersion}
                onChange={(e) => patch('patchVersion', e.target.value)}
                placeholder="latest"
              />
            </label>

            <label className={styles.guideImportField}>
              <span className={styles.guideImportLabel}>세트 번호</span>
              <input
                className={styles.guideImportInput}
                min={1}
                type="number"
                value={form.setNumber}
                onChange={(e) => patch('setNumber', e.target.value)}
              />
            </label>

            <label className={styles.guideImportField}>
              <span className={styles.guideImportLabel}>Mutator</span>
              <input
                className={styles.guideImportInput}
                value={form.mutator}
                onChange={(e) => patch('mutator', e.target.value)}
                placeholder="TFTSet17"
              />
            </label>
          </div>

          <div className={styles.guideImportChecks}>
            <label className={styles.guideImportCheck}>
              <input
                type="checkbox"
                checked={form.includeChampions}
                onChange={(e) => patch('includeChampions', e.target.checked)}
              />
              <span>챔피언</span>
            </label>

            <label className={styles.guideImportCheck}>
              <input
                type="checkbox"
                checked={form.includeItems}
                onChange={(e) => patch('includeItems', e.target.checked)}
              />
              <span>아이템</span>
            </label>

            <label className={styles.guideImportCheck}>
              <input
                type="checkbox"
                checked={form.includeAugments}
                onChange={(e) => patch('includeAugments', e.target.checked)}
              />
              <span>증강체</span>
            </label>

            <label className={styles.guideImportCheck}>
              <input
                type="checkbox"
                checked={form.includeTraits}
                onChange={(e) => patch('includeTraits', e.target.checked)}
              />
              <span>특성</span>
            </label>
          </div>

          <div className={styles.guideImportActions}>
            {error && <span className={styles.guideImportError}>{error}</span>}
            <button className={styles.saveBtn} type="submit" disabled={importing}>
              {importing ? '가져오는 중...' : 'CDragon 가져오기'}
            </button>
          </div>
        </form>

        {result && (
          <div className={styles.guideImportResult}>
            <span className={styles.guideImportStatus}>가져오기 완료</span>
            <div className={styles.guideImportResultGrid}>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>반영</span>
                <strong className={styles.guideImportMetricValue}>{importedCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>생성</span>
                <strong className={styles.guideImportMetricValue}>{result.createdCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>수정</span>
                <strong className={styles.guideImportMetricValue}>{result.updatedCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>스킵</span>
                <strong className={styles.guideImportMetricValue}>{result.skippedCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>챔피언 후보</span>
                <strong className={styles.guideImportMetricValue}>{result.championCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>특성 후보</span>
                <strong className={styles.guideImportMetricValue}>{result.traitCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>아이템 후보</span>
                <strong className={styles.guideImportMetricValue}>{result.itemCount}</strong>
              </div>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>증강체 후보</span>
                <strong className={styles.guideImportMetricValue}>{result.augmentCount}</strong>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminGuides
