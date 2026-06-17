import type { LucideIcon } from 'lucide-react'
import { ChevronRight, Shield, Sparkles, Swords, Wand2, Zap } from 'lucide-react'
import {
  type ChangeCategory,
  type ChangeType,
  type ImpactLevel,
  type PatchChange,
} from '../../../api/patchNotes'
import styles from '../PatchNotes.module.css'

const CATEGORY_ICON: Record<ChangeCategory, LucideIcon> = {
  챔피언: Swords,
  시너지: Shield,
  아이템: Wand2,
  증강체: Sparkles,
  시스템: Zap,
}

const CHANGE_TYPE_CLASS: Record<ChangeType, string> = {
  상향: styles.buff,
  하향: styles.nerf,
  조정: styles.adjust,
  신규: styles.new,
}

const IMPACT_CLASS: Record<ImpactLevel, string> = {
  높음: styles.highImpact,
  중간: styles.midImpact,
  낮음: styles.lowImpact,
}

const GENERIC_TARGET_NAMES = new Set([
  '기타',
  '버그 수정',
  '변경사항',
  '시스템',
  '시작 조우자',
  '아이템',
  '유닛',
  '증강',
  '증강체',
  '챔피언',
  '특성',
])

interface PatchChangeListProps {
  expandedChangeIds: number[]
  patchChanges: PatchChange[]
  onToggleExpandedChange: (id: number) => void
}

function getChangeTitle(change: PatchChange) {
  const target = change.target.trim()
  const summary = change.summary.trim()

  if (!target) return summary || '패치 변경사항'
  if (GENERIC_TARGET_NAMES.has(target) && summary) return summary
  return target
}

function getChangeSummary(change: PatchChange, title: string) {
  const summary = change.summary.trim()
  return summary && summary !== title ? summary : ''
}

function PatchChangeList({
  expandedChangeIds,
  onToggleExpandedChange,
  patchChanges,
}: PatchChangeListProps) {
  return (
    <div className={styles.changeList}>
      {patchChanges.map((change) => {
        const CategoryIcon = CATEGORY_ICON[change.category]
        const isExpanded = expandedChangeIds.includes(change.id)
        const title = getChangeTitle(change)
        const summary = getChangeSummary(change, title)

        return (
          <article key={change.id} className={styles.changeItem}>
            <div className={styles.changeTop}>
              <span className={styles.categoryBadge}>
                <CategoryIcon size={15} />
                {change.category}
              </span>
              <span className={`${styles.changeType} ${CHANGE_TYPE_CLASS[change.type]}`}>{change.type}</span>
              <span className={`${styles.impactBadge} ${IMPACT_CLASS[change.impact]}`}>영향 {change.impact}</span>
            </div>

            <div className={styles.changeBody}>
              <div className={styles.changeText}>
                <h3>{title}</h3>
                {summary && <p>{summary}</p>}
              </div>
              <button
                type="button"
                className={styles.detailButton}
                onClick={() => onToggleExpandedChange(change.id)}
                aria-expanded={isExpanded}
              >
                {isExpanded ? '접기' : '상세 보기'}
                <ChevronRight size={16} className={isExpanded ? styles.expandedArrow : undefined} />
              </button>
            </div>

            {isExpanded && (
              <div className={styles.compareGrid}>
                <div>
                  <span>이전</span>
                  <p>{change.before}</p>
                </div>
                <div>
                  <span>변경</span>
                  <p>{change.after}</p>
                </div>
              </div>
            )}

            {change.tags.length > 0 && (
              <div className={styles.tagRow}>
                {change.tags.map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
            )}
          </article>
        )
      })}

      {patchChanges.length === 0 && (
        <div className={styles.emptyState}>
          검색 조건에 맞는 패치 변경사항이 없습니다.
        </div>
      )}
    </div>
  )
}

export default PatchChangeList
