import type { LucideIcon } from 'lucide-react'
import { Shield, Sparkles, Swords, Wand2, Zap } from 'lucide-react'
import {
  CHANGE_CATEGORIES,
  type ChangeCategory,
  type ChangeType,
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
  patchChanges: PatchChange[]
}

interface PatchChangeGroup {
  title: string
  changes: PatchChange[]
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

function getChangeGroupKey(title: string) {
  return title.trim().toLowerCase()
}

function groupChangesByTitle(changes: PatchChange[]): PatchChangeGroup[] {
  const groups = new Map<string, PatchChangeGroup>()

  changes.forEach((change) => {
    const title = getChangeTitle(change)
    const key = getChangeGroupKey(title)
    const group = groups.get(key)

    if (group) {
      group.changes.push(change)
      return
    }

    groups.set(key, {
      title,
      changes: [change],
    })
  })

  return Array.from(groups.values())
}

function getChangeGroupTypes(changes: PatchChange[]) {
  return Array.from(new Set(changes.map((change) => change.type)))
}

function groupChangesByCategory(patchChanges: PatchChange[]) {
  return CHANGE_CATEGORIES
    .map((category) => ({
      category,
      changes: patchChanges.filter((change) => change.category === category),
    }))
    .filter((section) => section.changes.length > 0)
}

function PatchChangeList({ patchChanges }: PatchChangeListProps) {
  const sections = groupChangesByCategory(patchChanges).map((section) => ({
    ...section,
    groups: groupChangesByTitle(section.changes),
  }))

  return (
    <div className={styles.changeTimeline}>
      {sections.map((section) => {
        const CategoryIcon = CATEGORY_ICON[section.category]

        return (
          <section key={section.category} className={styles.changeSection}>
            <div className={styles.changeSectionHeader}>
              <span>
                <CategoryIcon size={16} />
                {section.category}
              </span>
              <strong>{section.changes.length}</strong>
            </div>

            <ul className={styles.changeBulletList}>
              {section.groups.map((group) => {
                const changeTypes = getChangeGroupTypes(group.changes)
                const changeDetails = group.changes
                  .map((change) => ({
                    change,
                    hasValueChange: Boolean(change.before || change.after),
                    summary: getChangeSummary(change, group.title),
                  }))
                  .filter((changeDetail) => changeDetail.summary || changeDetail.hasValueChange)

                return (
                  <li key={`${section.category}-${group.title}`} className={styles.changeTargetGroup}>
                    <div className={styles.changeTargetHeader}>
                      <div className={styles.changeTargetTitle}>
                        <strong>{group.title}</strong>
                        {group.changes.length > 1 && (
                          <span className={styles.changeGroupCount}>{group.changes.length}개</span>
                        )}
                      </div>
                      <div className={styles.changeTypeStack}>
                        {changeTypes.map((changeType) => (
                          <span
                            key={changeType}
                            className={`${styles.changeType} ${CHANGE_TYPE_CLASS[changeType]}`}
                          >
                            {changeType}
                          </span>
                        ))}
                      </div>
                    </div>

                    {changeDetails.length > 0 && (
                      <ul className={styles.changeDetailList}>
                        {changeDetails.map(({ change, hasValueChange, summary }) => (
                          <li key={change.id} className={styles.changeDetailItem}>
                            {summary && <p className={styles.changeDetailSummary}>{summary}</p>}
                            {hasValueChange && (
                              <p className={styles.changeValueLine}>
                                <span>{change.before || '-'}</span>
                                <strong>→</strong>
                                <span>{change.after || '-'}</span>
                              </p>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                )
              })}
            </ul>
          </section>
        )
      })}

      {sections.length === 0 && (
        <div className={styles.emptyState}>
          검색 조건에 맞는 패치 변경사항이 없습니다.
        </div>
      )}
    </div>
  )
}

export default PatchChangeList
