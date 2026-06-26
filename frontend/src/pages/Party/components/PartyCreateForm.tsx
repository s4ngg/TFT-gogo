import { type FormEvent, type RefObject } from 'react'
import type { PartyMode } from '../types'
import styles from '../Party.module.css'

interface PartyCreateFormProps {
  capacityDraft: string
  composeError: string
  deadlineDraft: string
  descriptionDraft: string
  formId: string
  isAuthenticated: boolean
  isSubmitting: boolean
  minDeadline: string
  modeDraft: PartyMode
  onCapacityChange: (value: string) => void
  onDeadlineChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onModeChange: (value: PartyMode) => void
  onSubmit: () => void
  onTagsChange: (value: string) => void
  onTierChange: (value: string) => void
  onTitleChange: (value: string) => void
  tagsDraft: string
  tierDraft: string
  titleDraft: string
  titleInputRef: RefObject<HTMLInputElement>
}

function PartyCreateForm({
  capacityDraft,
  composeError,
  deadlineDraft,
  descriptionDraft,
  formId,
  isAuthenticated,
  isSubmitting,
  minDeadline,
  modeDraft,
  onCapacityChange,
  onDeadlineChange,
  onDescriptionChange,
  onModeChange,
  onSubmit,
  onTagsChange,
  onTierChange,
  onTitleChange,
  tagsDraft,
  tierDraft,
  titleDraft,
  titleInputRef,
}: PartyCreateFormProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSubmit()
  }

  return (
    <form className={styles.composeBox} id={formId} onSubmit={handleSubmit}>
      <input
        aria-label="모집글 제목"
        onChange={(event) => onTitleChange(event.target.value)}
        placeholder="모집글 제목"
        ref={titleInputRef}
        value={titleDraft}
      />
      <select
        aria-label="모집 모드"
        onChange={(event) => onModeChange(event.target.value as PartyMode)}
        value={modeDraft}
      >
        <option value="랭크">랭크</option>
        <option value="일반">일반</option>
        <option value="커스텀">커스텀</option>
      </select>
      <select
        aria-label="티어 조건"
        onChange={(event) => onTierChange(event.target.value)}
        value={tierDraft}
      >
        <option value="마스터+">마스터+</option>
        <option value="다이아+">다이아+</option>
        <option value="플래티넘+">플래티넘+</option>
        <option value="제한 없음">제한 없음</option>
      </select>
      <input
        aria-label="모집 인원"
        onChange={(event) => onCapacityChange(event.target.value)}
        placeholder="모집 인원"
        value={capacityDraft}
      />
      <input
        aria-label="마감 시간"
        className={styles.deadlineInput}
        min={minDeadline}
        onChange={(event) => onDeadlineChange(event.target.value)}
        type="datetime-local"
        value={deadlineDraft}
      />
      <input
        aria-label="모집 태그"
        className={styles.tagInput}
        onChange={(event) => onTagsChange(event.target.value)}
        placeholder="태그 입력 예: 음성 가능, 연습 목표, 빠른 매칭"
        value={tagsDraft}
      />
      <textarea
        aria-label="모집 메모"
        onChange={(event) => onDescriptionChange(event.target.value)}
        placeholder="플레이 스타일이나 요청사항을 적어주세요."
        value={descriptionDraft}
      />
      <button
        type="submit"
        className={styles.primaryButton}
        disabled={isSubmitting || !isAuthenticated}
      >
        {!isAuthenticated ? '로그인 후 등록' : isSubmitting ? '등록중' : '등록'}
      </button>
      {composeError && (
        <p className={styles.composeError} role="alert">
          {composeError}
        </p>
      )}
    </form>
  )
}

export default PartyCreateForm
