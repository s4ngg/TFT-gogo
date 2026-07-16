import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Edit3, Plus, RefreshCcw, Save, Trash2 } from 'lucide-react'
import {
  createAdminClientVersionPatchMapping,
  deleteAdminClientVersionPatchMapping,
  fetchAdminClientVersionPatchMappings,
  isAdminAuthFailure,
  isNetworkOrTimeoutError,
  getServerErrorStatus,
  updateAdminClientVersionPatchMapping,
  type AdminClientVersionPatchMapping,
  type AdminClientVersionPatchMappingPayload,
} from '../../../api/adminApi'
import styles from '../AdminClientVersionPatchMappings.module.css'

const MAPPING_QUERY_KEY = ['admin', 'client-version-patch-mappings'] as const
const EMPTY_MAPPINGS: AdminClientVersionPatchMapping[] = []

interface MappingFormState {
  clientVersion: string
  patchVersion: string
}

const EMPTY_MAPPING_FORM: MappingFormState = {
  clientVersion: '',
  patchVersion: '',
}

function getAdminListErrorMessage(error: unknown, fallback: string): string {
  if (isAdminAuthFailure(error)) return '인증 실패: 관리자 토큰을 확인해 주세요.'
  if (isNetworkOrTimeoutError(error)) return '네트워크 오류: 연결 상태를 확인 후 다시 시도해 주세요.'
  const status = getServerErrorStatus(error)
  if (status != null) return `서버 오류가 발생했습니다. (${status})`
  return fallback
}

function getAdminSaveErrorMessage(error: unknown, fallback: string): string {
  if (isAdminAuthFailure(error)) return '인증 실패: 관리자 토큰을 확인해 주세요.'
  const status = getServerErrorStatus(error)
  if (status === 409) return '이미 등록된 클라이언트 버전입니다.'
  if (isNetworkOrTimeoutError(error)) return '네트워크 오류: 연결 상태를 확인 후 다시 시도해 주세요.'
  return fallback
}

function toMappingForm(mapping: AdminClientVersionPatchMapping): MappingFormState {
  return {
    clientVersion: mapping.clientVersion,
    patchVersion: mapping.patchVersion,
  }
}

function buildMappingPayload(form: MappingFormState): AdminClientVersionPatchMappingPayload | null {
  const clientVersion = form.clientVersion.trim()
  const patchVersion = form.patchVersion.trim()
  if (!clientVersion || !patchVersion) return null

  return { clientVersion, patchVersion }
}

function AdminClientVersionPatchMappingsManager() {
  const queryClient = useQueryClient()
  const [editingMappingId, setEditingMappingId] = useState<number | null>(null)
  const [mappingForm, setMappingForm] = useState<MappingFormState>(EMPTY_MAPPING_FORM)
  const [showForm, setShowForm] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const mappingsQuery = useQuery({
    queryFn: fetchAdminClientVersionPatchMappings,
    queryKey: MAPPING_QUERY_KEY,
  })

  const mappings = mappingsQuery.data ?? EMPTY_MAPPINGS

  const refreshMappings = async () => {
    await queryClient.invalidateQueries({ queryKey: MAPPING_QUERY_KEY })
  }

  const createMutation = useMutation({
    mutationFn: createAdminClientVersionPatchMapping,
    onSuccess: async () => {
      closeForm()
      setMessage('매핑을 생성했습니다.')
      await refreshMappings()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AdminClientVersionPatchMappingPayload }) =>
      updateAdminClientVersionPatchMapping(id, payload),
    onSuccess: async () => {
      closeForm()
      setMessage('매핑을 수정했습니다.')
      await refreshMappings()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteAdminClientVersionPatchMapping,
    onSuccess: async () => {
      setMessage('매핑을 삭제했습니다.')
      await refreshMappings()
    },
  })

  function clearNotice() {
    setError('')
    setMessage('')
  }

  function startNew() {
    clearNotice()
    setEditingMappingId(null)
    setMappingForm(EMPTY_MAPPING_FORM)
    setShowForm(true)
  }

  function startEdit(mapping: AdminClientVersionPatchMapping) {
    clearNotice()
    setEditingMappingId(mapping.id)
    setMappingForm(toMappingForm(mapping))
    setShowForm(true)
  }

  function closeForm() {
    setEditingMappingId(null)
    setMappingForm(EMPTY_MAPPING_FORM)
    setShowForm(false)
  }

  function updateForm<K extends keyof MappingFormState>(key: K, value: MappingFormState[K]) {
    setMappingForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearNotice()

    const payload = buildMappingPayload(mappingForm)
    if (!payload) {
      setError('클라이언트 버전과 패치 번호는 필수입니다.')
      return
    }

    try {
      if (editingMappingId) {
        await updateMutation.mutateAsync({ id: editingMappingId, payload })
      } else {
        await createMutation.mutateAsync(payload)
      }
    } catch (err) {
      setError(getAdminSaveErrorMessage(err, '매핑 저장에 실패했습니다. 입력값을 확인해주세요.'))
    }
  }

  async function handleDelete(mapping: AdminClientVersionPatchMapping) {
    if (!confirm(`${mapping.clientVersion} → ${mapping.patchVersion} 매핑을 삭제할까요?`)) return
    clearNotice()

    try {
      await deleteMutation.mutateAsync(mapping.id)
    } catch (err) {
      setError(getAdminSaveErrorMessage(err, '매핑 삭제에 실패했습니다.'))
    }
  }

  const isSaving = createMutation.isPending || updateMutation.isPending

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Patch Version Mapping Admin</p>
          <h1 className={styles.title}>패치 버전 매핑 관리</h1>
        </div>
        <button className={styles.secondaryButton} type="button" onClick={() => void refreshMappings()}>
          <RefreshCcw size={16} />
          새로고침
        </button>
      </header>

      <p className={styles.panelHint}>
        Riot 클라이언트 빌드 버전(예: 16.13)을 TFT 패치 번호(예: 17.6)로 변환하는 매핑입니다. 등록/수정/삭제하면
        다음 조회부터 메타덱의 표시용 패치 번호에 바로 반영됩니다.
      </p>

      {(message || error) && (
        <div className={error ? styles.errorBanner : styles.successBanner} role="status">
          <p className={styles.noticeText}>{error || message}</p>
        </div>
      )}

      <section className={styles.panel}>
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle}>매핑 목록</h2>
          <button className={styles.primaryButton} type="button" onClick={startNew}>
            <Plus size={16} />
            새 매핑
          </button>
        </div>

        {mappingsQuery.isLoading ? (
          <p className={styles.emptyText}>불러오는 중입니다.</p>
        ) : mappingsQuery.isError ? (
          <div>
            <p className={styles.errorBanner} role="alert">
              {getAdminListErrorMessage(mappingsQuery.error, '매핑 목록을 불러오지 못했습니다.')}
            </p>
            <button className={styles.secondaryButton} onClick={() => void refreshMappings()}>
              다시 불러오기
            </button>
          </div>
        ) : mappings.length === 0 ? (
          <p className={styles.emptyText}>등록된 매핑이 없습니다.</p>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>클라이언트 버전</th>
                <th>패치 번호</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {mappings.map((mapping) => (
                <tr key={mapping.id}>
                  <td>{mapping.clientVersion}</td>
                  <td>{mapping.patchVersion}</td>
                  <td>
                    <div className={styles.rowActions}>
                      <button className={styles.secondaryButton} type="button" onClick={() => startEdit(mapping)}>
                        <Edit3 size={15} />
                        수정
                      </button>
                      <button
                        className={styles.dangerButton}
                        type="button"
                        onClick={() => void handleDelete(mapping)}
                      >
                        <Trash2 size={15} />
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {showForm && (
        <form className={styles.panel} onSubmit={handleSubmit}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{editingMappingId ? '매핑 수정' : '매핑 생성'}</h2>
            <div className={styles.actionsEnd}>
              <button className={styles.secondaryButton} type="button" onClick={closeForm}>
                취소
              </button>
              <button className={styles.primaryButton} type="submit" disabled={isSaving}>
                <Save size={16} />
                {isSaving ? '저장 중' : '저장'}
              </button>
            </div>
          </div>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>클라이언트 버전</span>
              <input
                value={mappingForm.clientVersion}
                onChange={(event) => updateForm('clientVersion', event.target.value)}
                placeholder="16.13"
              />
            </label>
            <label className={styles.field}>
              <span>패치 번호</span>
              <input
                value={mappingForm.patchVersion}
                onChange={(event) => updateForm('patchVersion', event.target.value)}
                placeholder="17.6"
              />
            </label>
          </div>
        </form>
      )}
    </div>
  )
}

export default AdminClientVersionPatchMappingsManager
