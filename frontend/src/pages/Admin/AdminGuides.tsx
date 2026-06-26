import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download, RefreshCcw } from 'lucide-react'
import {
  fetchAdminPatchNotes,
  importGuideCdragonData,
  type AdminPatchNote,
  type GuideCdragonImportRequest,
  type GuideImportResponse,
} from '../../api/adminApi'
import { getEstimatedGuideImportProgress } from './utils/guideImportProgress'
import { isLatestGuideImportPatchVersion, resolveGuideImportPatchVersion } from './utils/guideImportVersion'
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

const ADMIN_PATCH_NOTES_QUERY_KEY = ['admin', 'patch-notes'] as const
const EMPTY_PATCH_NOTES: AdminPatchNote[] = []

function AdminGuides() {
  const [form, setForm] = useState<GuideImportFormState>(DEFAULT_GUIDE_IMPORT_FORM)
  const [result, setResult] = useState<GuideImportResponse | null>(null)
  const [error, setError] = useState('')
  const [importing, setImporting] = useState(false)
  const [importElapsedSeconds, setImportElapsedSeconds] = useState(0)

  const patchNotesQuery = useQuery({
    queryFn: fetchAdminPatchNotes,
    queryKey: ADMIN_PATCH_NOTES_QUERY_KEY,
  })

  const patchNotes = patchNotesQuery.data ?? EMPTY_PATCH_NOTES
  const currentPatchVersion = useMemo(
    () => patchNotes.find((note) => note.isCurrent)?.version ?? patchNotes[0]?.version ?? null,
    [patchNotes],
  )
  const resolvedPatchVersion = resolveGuideImportPatchVersion(form.patchVersion, currentPatchVersion)
  const isLatestPatchVersion = isLatestGuideImportPatchVersion(form.patchVersion)
  const isCheckingPatchVersion = isLatestPatchVersion && patchNotesQuery.isFetching
  const importProgressValue = getEstimatedGuideImportProgress(importElapsedSeconds)
  const importProgressStatus = isCheckingPatchVersion
    ? '최신 패치 버전 확인 중'
    : 'CDragon 데이터 수집 및 저장 중'

  useEffect(() => {
    if (!importing) {
      setImportElapsedSeconds(0)
      return undefined
    }

    const startedAt = Date.now()
    setImportElapsedSeconds(0)
    const timerId = window.setInterval(() => {
      setImportElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000))
    }, 1000)

    return () => window.clearInterval(timerId)
  }, [importing])

  function patch<K extends keyof GuideImportFormState>(key: K, value: GuideImportFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function buildPayload(patchVersionInput: string): GuideCdragonImportRequest | null {
    const patchVersion = patchVersionInput.trim()
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

  async function runImport(patchVersionInput: string) {
    const payload = buildPayload(patchVersionInput)
    if (!payload) return

    setImporting(true)
    setError('')
    setResult(null)
    try {
      if (isLatestGuideImportPatchVersion(patchVersionInput)) {
        await patchNotesQuery.refetch()
      }
      const response = await importGuideCdragonData(payload)
      setResult(response)
    } catch {
      setError('가져오기에 실패했습니다. 토큰, 패치 버전, 세트 정보를 확인하세요.')
    } finally {
      setImporting(false)
    }
  }

  async function handleImport(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    await runImport(resolvedPatchVersion)
  }

  async function handleLatestImportClick() {
    patch('patchVersion', 'latest')
    await runImport('latest')
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
                value={resolvedPatchVersion}
                onChange={(e) => patch('patchVersion', e.target.value)}
                placeholder={currentPatchVersion ?? 'latest'}
              />
              <span className={styles.guideImportHint}>
                {currentPatchVersion
                  ? `현재 패치노트 기준 ${currentPatchVersion}이 자동 적용됩니다.`
                  : patchNotesQuery.isFetching
                    ? '현재 패치 버전을 확인하는 중입니다.'
                  : '현재 패치노트가 없으면 latest 그대로 요청됩니다.'}
              </span>
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
            <button
              className={styles.resetBtn}
              type="button"
              disabled={importing}
              onClick={() => void handleLatestImportClick()}
            >
              <RefreshCcw size={14} />
              {isCheckingPatchVersion ? '패치 확인 중' : '최신 버전 가져오기'}
            </button>
            <button className={styles.saveBtn} type="submit" disabled={importing}>
              <Download size={14} />
              {importing ? '가져오는 중...' : '지정값으로 가져오기'}
            </button>
          </div>

          {importing && (
            <div className={styles.guideImportProgress} role="status" aria-live="polite">
              <div className={styles.guideImportProgressHeader}>
                <span>{importProgressStatus}</span>
                <strong>예상 {importProgressValue}%</strong>
              </div>
              <progress
                className={styles.guideImportProgressBar}
                max={100}
                value={importProgressValue}
              >
                {importProgressValue}%
              </progress>
              <span className={styles.guideImportProgressHint}>
                {importElapsedSeconds}초 경과 · 완료되면 결과 카드에 생성/수정/스킵 개수가 표시됩니다.
              </span>
            </div>
          )}
        </form>

        {result && (
          <div className={styles.guideImportResult}>
            <span className={styles.guideImportStatus}>가져오기 완료</span>
            <div className={styles.guideImportResultGrid}>
              <div className={styles.guideImportMetric}>
                <span className={styles.guideImportMetricLabel}>저장 패치</span>
                <strong className={styles.guideImportMetricValue}>{result.patchVersion}</strong>
              </div>
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
