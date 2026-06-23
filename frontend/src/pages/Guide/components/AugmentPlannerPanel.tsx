import type {
  AugmentPlan,
  AugmentPlanKey,
} from '../../../api/guide'
import styles from '../Guide.module.css'

interface AugmentPlannerPanelProps {
  augmentPlans: AugmentPlan[]
  onPlanKeyChange: (planKey: AugmentPlanKey) => void
  planKey: AugmentPlanKey
}

function AugmentPlannerPanel({
  augmentPlans,
  onPlanKeyChange,
  planKey,
}: AugmentPlannerPanelProps) {
  const selectedPlan = augmentPlans.find((plan) => plan.key === planKey) ?? augmentPlans[0]

  if (!selectedPlan) {
    return null
  }

  return (
    <section className={styles.plannerPanel}>
      <div className={styles.plannerTop}>
        <div>
          <span className={styles.sectionBadge}>선택 기준</span>
          <h2>증강 선택 기준</h2>
        </div>
        <div className={styles.planTabs} aria-label="증강 운영 기준 선택" role="tablist">
          {augmentPlans.map((plan) => (
            <button
              aria-selected={plan.key === planKey}
              className={plan.key === planKey ? styles.planActive : ''}
              key={plan.key}
              onClick={() => onPlanKeyChange(plan.key)}
              role="tab"
              type="button"
            >
              {plan.label}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.plannerBody}>
        <div className={styles.stageCards}>
          {selectedPlan.stages.map((stage) => (
            <article className={styles.stageCard} key={`${selectedPlan.key}-${stage.stage}`}>
              <span>{stage.stage}</span>
              <strong>{stage.choice}</strong>
              <p>{stage.focus}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

export default AugmentPlannerPanel
