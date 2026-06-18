import type { LucideIcon } from 'lucide-react'
import { Shield, Sparkles, Swords, Wand2, Zap } from 'lucide-react'
import {
  CHANGE_CATEGORIES,
  type ChangeCategory,
  type ChangeType,
  type ChangeTypeFilter,
  type PatchChange,
} from '../../../api/patchNotes'
import {
  getPatchChangeDetailSummary,
  getPatchChangeGroupKey,
  getVisiblePatchChangeTypes,
  groupPatchChangesByTitle,
  shouldShowPatchChangeValueLine,
} from '../utils/patchChangeDisplay'
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

interface PatchChangeListProps {
  activeChangeType: ChangeTypeFilter
  patchChanges: PatchChange[]
}

function groupChangesByCategory(patchChanges: PatchChange[]) {
  return CHANGE_CATEGORIES
    .map((category) => ({
      category,
      changes: patchChanges.filter((change) => change.category === category),
    }))
    .filter((section) => section.changes.length > 0)
}

function PatchChangeList({ activeChangeType, patchChanges }: PatchChangeListProps) {
  const sections = groupChangesByCategory(patchChanges).map((section) => ({
    ...section,
    groups: groupPatchChangesByTitle(section.changes),
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
                const changeTypes = getVisiblePatchChangeTypes(group.changes, activeChangeType)
                const changeDetails = group.changes
                  .map((change) => ({
                    change,
                    showValueChange: shouldShowPatchChangeValueLine(change),
                    summary: getPatchChangeDetailSummary(change, group.title),
                  }))
                  .filter((changeDetail) => changeDetail.summary || changeDetail.showValueChange)

                return (
                  <li key={`${section.category}-${getPatchChangeGroupKey(group.title)}`} className={styles.changeTargetGroup}>
                    <div className={styles.changeTargetHeader}>
                      <div className={styles.changeTargetTitle}>
                        <strong>{group.title}</strong>
                        {group.changes.length > 1 && (
                          <span className={styles.changeGroupCount}>{group.changes.length}개</span>
                        )}
                      </div>
                      {changeTypes.length > 0 && (
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
                      )}
                    </div>

                    {changeDetails.length > 0 && (
                      <ul className={styles.changeDetailList}>
                        {changeDetails.map(({ change, showValueChange, summary }) => (
                          <li key={change.id} className={styles.changeDetailItem}>
                            {summary && <p className={styles.changeDetailSummary}>{summary}</p>}
                            {showValueChange && (
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
