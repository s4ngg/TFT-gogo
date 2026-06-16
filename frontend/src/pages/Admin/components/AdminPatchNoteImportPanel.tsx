import { useState, type FormEvent } from 'react'
import type {
  PatchNoteCrawlImportRequest,
  PatchNoteCrawlImportResponse,
} from '../../../api/adminApi'
import styles from '../AdminPatchNotes.module.css'

interface ImportFormState {
  dryRun: boolean
  forceOverwrite: boolean
  locale: string
  sourceUrl: string
  version: string
}

interface AdminPatchNoteImportPanelProps {
  importing: boolean
  onImport: (payload: PatchNoteCrawlImportRequest) => Promise<void>
  result: PatchNoteCrawlImportResponse | null
}

const DEFAULT_IMPORT_FORM: ImportFormState = {
  dryRun: true,
  forceOverwrite: false,
  locale: 'ko-kr',
  sourceUrl: '',
  version: '',
}

function trimToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function buildImportPayload(form: ImportFormState): PatchNoteCrawlImportRequest {
  return {
    dryRun: form.dryRun,
    forceOverwrite: form.forceOverwrite,
    locale: trimToNull(form.locale) ?? 'ko-kr',
    sourceUrl: trimToNull(form.sourceUrl),
    version: trimToNull(form.version),
  }
}

function AdminPatchNoteImportPanel({ importing, onImport, result }: AdminPatchNoteImportPanelProps) {
  const [form, setForm] = useState<ImportFormState>(DEFAULT_IMPORT_FORM)
  const [lastDryRunPayload, setLastDryRunPayload] = useState<PatchNoteCrawlImportRequest | null>(null)
  const [localError, setLocalError] = useState('')

  function patch<K extends keyof ImportFormState>(key: K, value: ImportFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
    setLastDryRunPayload(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLocalError('')

    const payload = buildImportPayload(form)
    if (!payload.dryRun && !confirm('실제 DB에 패치노트 import를 반영할까요?')) {
      return
    }

    try {
      await onImport(payload)
      setLastDryRunPayload(payload.dryRun ? payload : null)
    } catch {
      setLocalError('공식 패치노트 import에 실패했습니다. URL, 관리자 토큰, 서버 로그를 확인해주세요.')
    }
  }

  async function handleConfirmDryRunImport() {
    if (!lastDryRunPayload || importing) return
    setLocalError('')

    if (!confirm('미리 확인한 결과를 기준으로 실제 DB에 패치노트 import를 반영할까요?')) {
      return
    }

    try {
      await onImport({ ...lastDryRunPayload, dryRun: false })
      setLastDryRunPayload(null)
      setForm((prev) => ({ ...prev, dryRun: false }))
    } catch {
      setLocalError('미리 확인 결과 반영에 실패했습니다. URL, 관리자 토큰, 서버 로그를 확인해주세요.')
    }
  }

  const metrics = result
    ? [
        { label: '생성', value: result.createdCount },
        { label: '수정', value: result.updatedCount },
        { label: '스킵', value: result.skippedCount },
        { label: '검토 필요', value: result.reviewRequiredCount },
        { label: '실패', value: result.failedCount },
      ]
    : []
  const canConfirmDryRunImport = result?.dryRun === true && lastDryRunPayload !== null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <div>
          <h2 className={styles.panelTitle}>공식 패치노트 Import</h2>
          <p className={styles.panelHint}>Riot/TFT 공식 패치노트 URL 기준으로 미리 확인하거나 저장합니다.</p>
        </div>
      </div>

      <form className={styles.importForm} onSubmit={handleSubmit}>
        <div className={styles.formGrid}>
          <label className={styles.field}>
            <span>공식 상세 URL</span>
            <input
              value={form.sourceUrl}
              onChange={(event) => patch('sourceUrl', event.target.value)}
              placeholder="https://teamfighttactics.leagueoflegends.com/ko-kr/news/game-updates/..."
            />
          </label>
          <label className={styles.field}>
            <span>버전 override</span>
            <input
              value={form.version}
              onChange={(event) => patch('version', event.target.value)}
              placeholder="17.2"
            />
          </label>
        </div>

        <div className={styles.importOptions}>
          <label className={styles.field}>
            <span>Locale</span>
            <select value={form.locale} onChange={(event) => patch('locale', event.target.value)}>
              <option value="ko-kr">ko-kr</option>
              <option value="en-us">en-us</option>
            </select>
          </label>
          <label className={styles.checkboxField}>
            <input
              type="checkbox"
              checked={form.dryRun}
              onChange={(event) => patch('dryRun', event.target.checked)}
            />
            <span>미리 확인</span>
          </label>
          <label className={styles.checkboxField}>
            <input
              type="checkbox"
              checked={form.forceOverwrite}
              onChange={(event) => patch('forceOverwrite', event.target.checked)}
            />
            <span>수동 수정 덮어쓰기</span>
          </label>
        </div>

        <div className={styles.actionsEnd}>
          {localError && <span className={styles.inlineError}>{localError}</span>}
          <button className={styles.primaryButton} type="submit" disabled={importing}>
            {importing ? '처리 중' : form.dryRun ? '미리 확인' : 'Import 반영'}
          </button>
        </div>
      </form>

      {result && (
        <div className={styles.importResult}>
          <div className={styles.importMeta}>
            <span>{result.dryRun ? '미리 확인 완료' : 'Import 완료'}</span>
            <strong>{result.version}</strong>
            <span>{result.locale}</span>
            {result.patchNoteId !== null && <span>패치노트 ID {result.patchNoteId}</span>}
          </div>

          <div className={styles.importMetricGrid}>
            {metrics.map((metric) => (
              <div key={metric.label} className={styles.importMetric}>
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </div>
            ))}
          </div>

          {canConfirmDryRunImport && (
            <div className={styles.importConfirmBox}>
              <div>
                <strong>미리 확인 결과를 저장 반영할 수 있습니다.</strong>
                <p>입력값을 바꾸지 않은 상태에서 같은 sourceUrl, version, locale로 실제 import를 실행합니다.</p>
              </div>
              <button
                className={styles.primaryButton}
                type="button"
                disabled={importing}
                onClick={() => void handleConfirmDryRunImport()}
              >
                {importing ? '처리 중' : '결과 그대로 Import 반영'}
              </button>
            </div>
          )}

          {result.parserWarnings.length > 0 && (
            <div className={styles.importMessageBlock}>
              <h3>파서 경고</h3>
              <ul>
                {result.parserWarnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </div>
          )}

          {result.rowErrors.length > 0 && (
            <div className={styles.importMessageBlock}>
              <h3>행 오류</h3>
              <div className={styles.rowErrorTableWrap}>
                <table className={styles.rowErrorTable}>
                  <thead>
                    <tr>
                      <th>순서</th>
                      <th>위치</th>
                      <th>내용</th>
                      <th>사유</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.rowErrors.map((rowError) => (
                      <tr key={`${rowError.sourceKey ?? 'row'}-${rowError.sourceOrder ?? 'unknown'}`}>
                        <td>{rowError.sourceOrder ?? '-'}</td>
                        <td>{rowError.headingPath ?? '-'}</td>
                        <td>{rowError.rowTextPreview ?? '-'}</td>
                        <td>{rowError.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

export default AdminPatchNoteImportPanel
