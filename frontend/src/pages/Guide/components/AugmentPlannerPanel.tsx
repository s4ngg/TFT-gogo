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

  return (
    <section className={styles.plannerPanel}>
      <div className={styles.plannerTop}>
        <div>
          <span className={styles.sectionBadge}>배치툴</span>
          <h2>증강 선택 플랜</h2>
        </div>
        <div className={styles.planTabs}>
          {augmentPlans.map((plan) => (
            <button
              className={plan.key === planKey ? styles.planActive : ''}
              key={plan.key}
              onClick={() => onPlanKeyChange(plan.key)}
              type="button"
            >
              {plan.label}
            </button>
          ))}
        </div>
      </div>

      {selectedPlan && (
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
          <div className={styles.boardTool} aria-label="증강 선택 이후 배치 미리보기">
            {Array.from({ length: 21 }).map((_, index) => (
              <span
                className={
                  index === 2 || index === 4 || index === 10 || index === 16
                    ? styles.boardCellActive
                    : ''
                }
                key={index}
              />
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

export default AugmentPlannerPanel
