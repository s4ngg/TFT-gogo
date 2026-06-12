import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Edit3, FileText, Plus, RefreshCcw, Save, Trash2 } from 'lucide-react'
import {
  createAdminPatchChange,
  createAdminPatchNote,
  deleteAdminPatchChange,
  deleteAdminPatchNote,
  fetchAdminPatchChanges,
  fetchAdminPatchNotes,
  updateAdminPatchChange,
  updateAdminPatchNote,
  type AdminPatchChange,
  type AdminPatchChangeCategory,
  type AdminPatchChangeImpact,
  type AdminPatchChangePayload,
  type AdminPatchChangeType,
  type AdminPatchNote,
  type AdminPatchNotePayload,
} from '../../../api/adminApi'
import styles from '../AdminPatchNotes.module.css'

const PATCH_NOTE_QUERY_KEY = ['admin', 'patch-notes'] as const
const PATCH_CHANGE_QUERY_KEY = ['admin', 'patch-note-changes'] as const
const EMPTY_PATCH_NOTES: AdminPatchNote[] = []

const CATEGORY_OPTIONS: Array<{ label: string; value: AdminPatchChangeCategory }> = [
  { label: '챔피언', value: 'CHAMPION' },
  { label: '시너지', value: 'TRAIT' },
  { label: '아이템', value: 'ITEM' },
  { label: '증강체', value: 'AUGMENT' },
  { label: '시스템', value: 'SYSTEM' },
]

const CHANGE_TYPE_OPTIONS: Array<{ label: string; value: AdminPatchChangeType }> = [
  { label: '상향', value: 'BUFF' },
  { label: '하향', value: 'NERF' },
  { label: '조정', value: 'ADJUST' },
  { label: '신규', value: 'NEW' },
]

const IMPACT_OPTIONS: Array<{ label: string; value: AdminPatchChangeImpact }> = [
  { label: '높음', value: 'HIGH' },
  { label: '중간', value: 'MEDIUM' },
  { label: '낮음', value: 'LOW' },
]

interface PatchNoteFormState {
  current: boolean
  description: string
  focus: string
  highlights: string
  imageUrl: string
  publishedAt: string
  summary: string
  title: string
  version: string
}

interface PatchChangeFormState {
  afterValue: string
  beforeValue: string
  category: AdminPatchChangeCategory
  imageUrl: string
  impact: AdminPatchChangeImpact
  sortOrder: string
  summary: string
  tags: string
  targetKey: string
  targetName: string
  type: AdminPatchChangeType
}

const EMPTY_PATCH_NOTE_FORM: PatchNoteFormState = {
  current: false,
  description: '',
  focus: '',
  highlights: '',
  imageUrl: '',
  publishedAt: '',
  summary: '',
  title: '',
  version: '',
}

const EMPTY_PATCH_CHANGE_FORM: PatchChangeFormState = {
  afterValue: '',
  beforeValue: '',
  category: 'CHAMPION',
  imageUrl: '',
  impact: 'MEDIUM',
  sortOrder: '0',
  summary: '',
  tags: '',
  targetKey: '',
  targetName: '',
  type: 'ADJUST',
}

function trimToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function splitLines(value: string): string[] {
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

function joinLines(values: string[] | null | undefined): string {
  return values?.join('\n') ?? ''
}

function toDateTimeLocal(value: string | null | undefined): string {
  return value ? value.slice(0, 16) : ''
}

function getCategoryLabel(value: AdminPatchChangeCategory): string {
  return CATEGORY_OPTIONS.find((option) => option.value === value)?.label ?? value
}

function getChangeTypeLabel(value: AdminPatchChangeType): string {
  return CHANGE_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value
}

function getImpactLabel(value: AdminPatchChangeImpact): string {
  return IMPACT_OPTIONS.find((option) => option.value === value)?.label ?? value
}

function toPatchNoteForm(note: AdminPatchNote): PatchNoteFormState {
  return {
    current: note.isCurrent,
    description: note.description ?? '',
    focus: note.focus ?? '',
    highlights: joinLines(note.highlights),
    imageUrl: note.imageUrl ?? '',
    publishedAt: toDateTimeLocal(note.publishedAt),
    summary: note.summary,
    title: note.title,
    version: note.version,
  }
}

function toPatchChangeForm(change: AdminPatchChange, sortOrder: number): PatchChangeFormState {
  return {
    afterValue: change.afterValue ?? '',
    beforeValue: change.beforeValue ?? '',
    category: change.category,
    imageUrl: change.imageUrl ?? '',
    impact: change.impact,
    sortOrder: String(sortOrder),
    summary: change.summary,
    tags: joinLines(change.tags),
    targetKey: change.targetKey,
    targetName: change.targetName,
    type: change.type,
  }
}

function buildPatchNotePayload(form: PatchNoteFormState): AdminPatchNotePayload | null {
  const version = form.version.trim()
  const title = form.title.trim()
  const summary = form.summary.trim()
  const publishedAt = form.publishedAt.trim()

  if (!version || !title || !summary || !publishedAt) return null

  return {
    current: form.current,
    description: trimToNull(form.description),
    focus: trimToNull(form.focus),
    highlights: splitLines(form.highlights),
    imageUrl: trimToNull(form.imageUrl),
    publishedAt,
    summary,
    title,
    version,
  }
}

function buildPatchChangePayload(
  form: PatchChangeFormState,
  patchNoteId: number,
): AdminPatchChangePayload | null {
  const sortOrder = Number(form.sortOrder)
  const targetKey = form.targetKey.trim()
  const targetName = form.targetName.trim()
  const summary = form.summary.trim()

  if (!Number.isInteger(sortOrder) || sortOrder < 0 || !targetKey || !targetName || !summary) {
    return null
  }

  return {
    afterValue: trimToNull(form.afterValue),
    beforeValue: trimToNull(form.beforeValue),
    category: form.category,
    imageUrl: trimToNull(form.imageUrl),
    impact: form.impact,
    patchNoteId,
    sortOrder,
    summary,
    tags: splitLines(form.tags),
    targetKey,
    targetName,
    type: form.type,
  }
}

function AdminPatchNotesManager() {
  const queryClient = useQueryClient()
  const [selectedPatchNoteId, setSelectedPatchNoteId] = useState<number | null>(null)
  const [editingPatchNoteId, setEditingPatchNoteId] = useState<number | null>(null)
  const [editingChangeId, setEditingChangeId] = useState<number | null>(null)
  const [patchNoteForm, setPatchNoteForm] = useState<PatchNoteFormState>(EMPTY_PATCH_NOTE_FORM)
  const [patchChangeForm, setPatchChangeForm] = useState<PatchChangeFormState>(EMPTY_PATCH_CHANGE_FORM)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const patchNotesQuery = useQuery({
    queryFn: fetchAdminPatchNotes,
    queryKey: PATCH_NOTE_QUERY_KEY,
  })

  const patchNotes = patchNotesQuery.data ?? EMPTY_PATCH_NOTES
  const selectedPatchNote = useMemo(
    () => patchNotes.find((note) => note.id === selectedPatchNoteId) ?? patchNotes[0] ?? null,
    [patchNotes, selectedPatchNoteId],
  )

  const patchChangesQuery = useQuery({
    enabled: selectedPatchNote !== null,
    queryFn: () => fetchAdminPatchChanges(selectedPatchNote?.version ?? ''),
    queryKey: [...PATCH_CHANGE_QUERY_KEY, selectedPatchNote?.version ?? ''],
  })

  const patchChanges = patchChangesQuery.data?.items ?? []

  useEffect(() => {
    if (patchNotes.length === 0) {
      setSelectedPatchNoteId(null)
      return
    }

    if (!selectedPatchNoteId || !patchNotes.some((note) => note.id === selectedPatchNoteId)) {
      setSelectedPatchNoteId(patchNotes[0].id)
    }
  }, [patchNotes, selectedPatchNoteId])

  const refreshPatchNotes = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: PATCH_NOTE_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: ['patch-notes'] }),
    ])
  }

  const refreshPatchChanges = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: PATCH_CHANGE_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: ['patch-notes'] }),
    ])
  }

  const createPatchNoteMutation = useMutation({
    mutationFn: createAdminPatchNote,
    onSuccess: async (note) => {
      setSelectedPatchNoteId(note.id)
      setEditingPatchNoteId(null)
      setPatchNoteForm(EMPTY_PATCH_NOTE_FORM)
      setMessage('패치노트를 생성했습니다.')
      await refreshPatchNotes()
    },
  })

  const updatePatchNoteMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AdminPatchNotePayload }) =>
      updateAdminPatchNote(id, payload),
    onSuccess: async (note) => {
      setSelectedPatchNoteId(note.id)
      setEditingPatchNoteId(null)
      setPatchNoteForm(EMPTY_PATCH_NOTE_FORM)
      setMessage('패치노트를 수정했습니다.')
      await refreshPatchNotes()
    },
  })

  const deletePatchNoteMutation = useMutation({
    mutationFn: deleteAdminPatchNote,
    onSuccess: async () => {
      setSelectedPatchNoteId(null)
      setEditingPatchNoteId(null)
      setPatchNoteForm(EMPTY_PATCH_NOTE_FORM)
      setMessage('패치노트를 삭제했습니다.')
      await refreshPatchNotes()
    },
  })

  const createPatchChangeMutation = useMutation({
    mutationFn: createAdminPatchChange,
    onSuccess: async () => {
      setEditingChangeId(null)
      setPatchChangeForm(EMPTY_PATCH_CHANGE_FORM)
      setMessage('변경사항을 생성했습니다.')
      await refreshPatchChanges()
      await refreshPatchNotes()
    },
  })

  const updatePatchChangeMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AdminPatchChangePayload }) =>
      updateAdminPatchChange(id, payload),
    onSuccess: async () => {
      setEditingChangeId(null)
      setPatchChangeForm(EMPTY_PATCH_CHANGE_FORM)
      setMessage('변경사항을 수정했습니다.')
      await refreshPatchChanges()
    },
  })

  const deletePatchChangeMutation = useMutation({
    mutationFn: deleteAdminPatchChange,
    onSuccess: async () => {
      setEditingChangeId(null)
      setPatchChangeForm(EMPTY_PATCH_CHANGE_FORM)
      setMessage('변경사항을 삭제했습니다.')
      await refreshPatchChanges()
      await refreshPatchNotes()
    },
  })

  function updatePatchNoteForm<K extends keyof PatchNoteFormState>(key: K, value: PatchNoteFormState[K]) {
    setPatchNoteForm((prev) => ({ ...prev, [key]: value }))
  }

  function updatePatchChangeForm<K extends keyof PatchChangeFormState>(key: K, value: PatchChangeFormState[K]) {
    setPatchChangeForm((prev) => ({ ...prev, [key]: value }))
  }

  function clearNotice() {
    setError('')
    setMessage('')
  }

  function startNewPatchNote() {
    clearNotice()
    setEditingPatchNoteId(null)
    setPatchNoteForm(EMPTY_PATCH_NOTE_FORM)
  }

  function startEditPatchNote(note: AdminPatchNote) {
    clearNotice()
    setSelectedPatchNoteId(note.id)
    setEditingPatchNoteId(note.id)
    setPatchNoteForm(toPatchNoteForm(note))
  }

  function startNewPatchChange() {
    clearNotice()
    setEditingChangeId(null)
    setPatchChangeForm({
      ...EMPTY_PATCH_CHANGE_FORM,
      sortOrder: String(patchChanges.length),
    })
  }

  function startEditPatchChange(change: AdminPatchChange, index: number) {
    clearNotice()
    setEditingChangeId(change.id)
    setPatchChangeForm(toPatchChangeForm(change, index))
  }

  async function handlePatchNoteSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearNotice()

    const payload = buildPatchNotePayload(patchNoteForm)
    if (!payload) {
      setError('버전, 제목, 요약, 공개일시는 필수입니다.')
      return
    }

    try {
      if (editingPatchNoteId) {
        await updatePatchNoteMutation.mutateAsync({ id: editingPatchNoteId, payload })
      } else {
        await createPatchNoteMutation.mutateAsync(payload)
      }
    } catch {
      setError('패치노트 저장에 실패했습니다. 입력값과 관리자 토큰을 확인해주세요.')
    }
  }

  async function handlePatchNoteDelete(note: AdminPatchNote) {
    if (!confirm(`${note.version} 패치노트를 삭제할까요? 연결된 변경사항도 함께 숨김 처리됩니다.`)) return
    clearNotice()

    try {
      await deletePatchNoteMutation.mutateAsync(note.id)
    } catch {
      setError('패치노트 삭제에 실패했습니다.')
    }
  }

  async function handlePatchChangeSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearNotice()

    if (!selectedPatchNote) {
      setError('먼저 패치노트를 선택해주세요.')
      return
    }

    const payload = buildPatchChangePayload(patchChangeForm, selectedPatchNote.id)
    if (!payload) {
      setError('대상 key, 대상 이름, 요약, 정렬 순서를 확인해주세요.')
      return
    }

    try {
      if (editingChangeId) {
        await updatePatchChangeMutation.mutateAsync({ id: editingChangeId, payload })
      } else {
        await createPatchChangeMutation.mutateAsync(payload)
      }
    } catch {
      setError('변경사항 저장에 실패했습니다. 입력값과 관리자 토큰을 확인해주세요.')
    }
  }

  async function handlePatchChangeDelete(change: AdminPatchChange) {
    if (!confirm(`${change.targetName} 변경사항을 삭제할까요?`)) return
    clearNotice()

    try {
      await deletePatchChangeMutation.mutateAsync(change.id)
    } catch {
      setError('변경사항 삭제에 실패했습니다.')
    }
  }

  const isPatchNoteSaving = createPatchNoteMutation.isPending || updatePatchNoteMutation.isPending
  const isPatchChangeSaving = createPatchChangeMutation.isPending || updatePatchChangeMutation.isPending

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Patch Notes Admin</p>
          <h1 className={styles.title}>패치노트 관리</h1>
        </div>
        <button className={styles.secondaryButton} type="button" onClick={() => void refreshPatchNotes()}>
          <RefreshCcw size={16} />
          새로고침
        </button>
      </header>

      {(message || error) && (
        <div className={error ? styles.errorBanner : styles.successBanner} role="status">
          {error || message}
        </div>
      )}

      <section className={styles.grid}>
        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>패치노트 목록</h2>
            <button className={styles.primaryButton} type="button" onClick={startNewPatchNote}>
              <Plus size={16} />
              새 패치
            </button>
          </div>

          {patchNotesQuery.isLoading ? (
            <p className={styles.emptyText}>불러오는 중입니다.</p>
          ) : patchNotes.length === 0 ? (
            <p className={styles.emptyText}>등록된 패치노트가 없습니다.</p>
          ) : (
            <div className={styles.noteList}>
              {patchNotes.map((note) => (
                <button
                  key={note.id}
                  className={`${styles.noteItem} ${selectedPatchNote?.id === note.id ? styles.noteItemActive : ''}`}
                  type="button"
                  onClick={() => setSelectedPatchNoteId(note.id)}
                >
                  <span className={styles.noteVersion}>{note.version}</span>
                  <span className={styles.noteTitle}>{note.title}</span>
                  <span className={styles.noteMeta}>
                    {note.isCurrent ? '현재 패치' : '이전 패치'} · 변경 {note.changeCount}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <form className={styles.panel} onSubmit={handlePatchNoteSubmit}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{editingPatchNoteId ? '패치노트 수정' : '패치노트 생성'}</h2>
            <div className={styles.actions}>
              {editingPatchNoteId && (
                <button className={styles.secondaryButton} type="button" onClick={startNewPatchNote}>
                  취소
                </button>
              )}
              <button className={styles.primaryButton} type="submit" disabled={isPatchNoteSaving}>
                <Save size={16} />
                {isPatchNoteSaving ? '저장 중' : '저장'}
              </button>
            </div>
          </div>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>버전</span>
              <input
                value={patchNoteForm.version}
                onChange={(event) => updatePatchNoteForm('version', event.target.value)}
                placeholder="17.3"
              />
            </label>
            <label className={styles.field}>
              <span>공개일시</span>
              <input
                type="datetime-local"
                value={patchNoteForm.publishedAt}
                onChange={(event) => updatePatchNoteForm('publishedAt', event.target.value)}
              />
            </label>
          </div>

          <label className={styles.field}>
            <span>제목</span>
            <input
              value={patchNoteForm.title}
              onChange={(event) => updatePatchNoteForm('title', event.target.value)}
              placeholder="17.3 패치노트"
            />
          </label>

          <label className={styles.field}>
            <span>요약</span>
            <textarea
              rows={3}
              value={patchNoteForm.summary}
              onChange={(event) => updatePatchNoteForm('summary', event.target.value)}
              placeholder="이번 패치의 핵심 변경 요약"
            />
          </label>

          <label className={styles.field}>
            <span>상세 설명</span>
            <textarea
              rows={3}
              value={patchNoteForm.description}
              onChange={(event) => updatePatchNoteForm('description', event.target.value)}
              placeholder="공개 페이지에 보여줄 상세 설명"
            />
          </label>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>포커스</span>
              <input
                value={patchNoteForm.focus}
                onChange={(event) => updatePatchNoteForm('focus', event.target.value)}
                placeholder="메타 변화 포인트"
              />
            </label>
            <label className={styles.field}>
              <span>대표 이미지 URL</span>
              <input
                value={patchNoteForm.imageUrl}
                onChange={(event) => updatePatchNoteForm('imageUrl', event.target.value)}
                placeholder="https://..."
              />
            </label>
          </div>

          <label className={styles.field}>
            <span>하이라이트</span>
            <textarea
              rows={4}
              value={patchNoteForm.highlights}
              onChange={(event) => updatePatchNoteForm('highlights', event.target.value)}
              placeholder="한 줄에 하나씩 입력"
            />
          </label>

          <label className={styles.checkboxField}>
            <input
              type="checkbox"
              checked={patchNoteForm.current}
              onChange={(event) => updatePatchNoteForm('current', event.target.checked)}
            />
            <span>현재 패치로 지정</span>
          </label>
        </form>
      </section>

      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <div>
            <h2 className={styles.panelTitle}>변경사항 관리</h2>
            <p className={styles.panelHint}>
              {selectedPatchNote
                ? `${selectedPatchNote.version} 패치에 연결됩니다.`
                : '패치노트를 먼저 선택해주세요.'}
            </p>
          </div>
          <button
            className={styles.primaryButton}
            type="button"
            disabled={!selectedPatchNote}
            onClick={startNewPatchChange}
          >
            <Plus size={16} />
            새 변경사항
          </button>
        </div>

        <div className={styles.changeLayout}>
          <form className={styles.changeForm} onSubmit={handlePatchChangeSubmit}>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>카테고리</span>
                <select
                  value={patchChangeForm.category}
                  onChange={(event) =>
                    updatePatchChangeForm('category', event.target.value as AdminPatchChangeCategory)
                  }
                >
                  {CATEGORY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
              <label className={styles.field}>
                <span>변경 유형</span>
                <select
                  value={patchChangeForm.type}
                  onChange={(event) =>
                    updatePatchChangeForm('type', event.target.value as AdminPatchChangeType)
                  }
                >
                  {CHANGE_TYPE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
              <label className={styles.field}>
                <span>영향도</span>
                <select
                  value={patchChangeForm.impact}
                  onChange={(event) =>
                    updatePatchChangeForm('impact', event.target.value as AdminPatchChangeImpact)
                  }
                >
                  {IMPACT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
            </div>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>대상 key</span>
                <input
                  value={patchChangeForm.targetKey}
                  onChange={(event) => updatePatchChangeForm('targetKey', event.target.value)}
                  placeholder="TFT17_Jinx"
                />
              </label>
              <label className={styles.field}>
                <span>대상 이름</span>
                <input
                  value={patchChangeForm.targetName}
                  onChange={(event) => updatePatchChangeForm('targetName', event.target.value)}
                  placeholder="징크스"
                />
              </label>
              <label className={styles.field}>
                <span>정렬 순서</span>
                <input
                  min={0}
                  type="number"
                  value={patchChangeForm.sortOrder}
                  onChange={(event) => updatePatchChangeForm('sortOrder', event.target.value)}
                />
              </label>
            </div>

            <label className={styles.field}>
              <span>요약</span>
              <textarea
                rows={3}
                value={patchChangeForm.summary}
                onChange={(event) => updatePatchChangeForm('summary', event.target.value)}
                placeholder="변경사항 요약"
              />
            </label>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>변경 전</span>
                <input
                  value={patchChangeForm.beforeValue}
                  onChange={(event) => updatePatchChangeForm('beforeValue', event.target.value)}
                  placeholder="기존 수치"
                />
              </label>
              <label className={styles.field}>
                <span>변경 후</span>
                <input
                  value={patchChangeForm.afterValue}
                  onChange={(event) => updatePatchChangeForm('afterValue', event.target.value)}
                  placeholder="변경 수치"
                />
              </label>
            </div>

            <label className={styles.field}>
              <span>이미지 URL</span>
              <input
                value={patchChangeForm.imageUrl}
                onChange={(event) => updatePatchChangeForm('imageUrl', event.target.value)}
                placeholder="https://..."
              />
            </label>

            <label className={styles.field}>
              <span>태그</span>
              <textarea
                rows={3}
                value={patchChangeForm.tags}
                onChange={(event) => updatePatchChangeForm('tags', event.target.value)}
                placeholder="한 줄에 하나씩 입력"
              />
            </label>

            <div className={styles.actionsEnd}>
              {editingChangeId && (
                <button className={styles.secondaryButton} type="button" onClick={startNewPatchChange}>
                  취소
                </button>
              )}
              <button className={styles.primaryButton} type="submit" disabled={!selectedPatchNote || isPatchChangeSaving}>
                <Save size={16} />
                {isPatchChangeSaving ? '저장 중' : '변경사항 저장'}
              </button>
            </div>
          </form>

          <div className={styles.changeList}>
            {patchChangesQuery.isFetching ? (
              <p className={styles.emptyText}>변경사항을 불러오는 중입니다.</p>
            ) : patchChanges.length === 0 ? (
              <p className={styles.emptyText}>등록된 변경사항이 없습니다.</p>
            ) : (
              patchChanges.map((change, index) => (
                <article key={change.id} className={styles.changeItem}>
                  <div className={styles.changeItemHeader}>
                    <div className={styles.changeTitleRow}>
                      <FileText size={16} />
                      <strong>{change.targetName}</strong>
                    </div>
                    <span className={styles.changeBadges}>
                      {getCategoryLabel(change.category)} · {getChangeTypeLabel(change.type)} · {getImpactLabel(change.impact)}
                    </span>
                  </div>
                  <p className={styles.changeSummary}>{change.summary}</p>
                  {(change.beforeValue || change.afterValue) && (
                    <p className={styles.changeValues}>
                      {change.beforeValue || '-'} → {change.afterValue || '-'}
                    </p>
                  )}
                  <div className={styles.changeActions}>
                    <button
                      className={styles.secondaryButton}
                      type="button"
                      onClick={() => startEditPatchChange(change, index)}
                    >
                      <Edit3 size={15} />
                      수정
                    </button>
                    <button
                      className={styles.dangerButton}
                      type="button"
                      onClick={() => void handlePatchChangeDelete(change)}
                    >
                      <Trash2 size={15} />
                      삭제
                    </button>
                  </div>
                </article>
              ))
            )}
          </div>
        </div>
      </section>

      {selectedPatchNote && (
        <div className={styles.footerActions}>
          <button className={styles.secondaryButton} type="button" onClick={() => startEditPatchNote(selectedPatchNote)}>
            <Edit3 size={16} />
            선택 패치 수정
          </button>
          <button className={styles.dangerButton} type="button" onClick={() => void handlePatchNoteDelete(selectedPatchNote)}>
            <Trash2 size={16} />
            선택 패치 삭제
          </button>
        </div>
      )}
    </div>
  )
}

export default AdminPatchNotesManager
