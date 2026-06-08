import { Swords } from 'lucide-react'
import { getChampionName, getItemName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import { tftItemIconUrl, tftItemIconOnError } from '../../../api/communityDragonAssets'
import type { MetaDeck } from '../../Dashboard/dashboardData'
import styles from '../DeckDetail.module.css'

interface ItemsPanelProps {
  deck: MetaDeck
  locale: TFTLocale | undefined
}

function ItemsPanel({ deck, locale }: ItemsPanelProps) {
  const carries = deck.champions
    .filter((champ) => (champ.recommendedItems?.length ?? 0) > 0)
    .slice(0, 3)

  if (carries.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Swords size={16} />
        <h2>추천 아이템</h2>
        <span className={styles.panelSub}>캐리 유닛별 핵심 3개</span>
      </div>
      <div className={styles.itemList}>
        {carries.map((champ) => (
          <div key={champ.name} className={styles.itemCard}>
            <div className={styles.itemChampCol}>
              <img
                src={champ.imageUrl}
                alt={champ.name}
                className={styles.itemChampImg}
                onError={(e) => { e.currentTarget.style.opacity = '0.3' }}
              />
              <span className={styles.itemChampName}>
                {getChampionName(champ.imageUrl, locale, champ.name)}
              </span>
            </div>
            <div className={styles.coreSection}>
              <span className={styles.itemSectionLabel}>핵심 아이템</span>
              <div className={styles.coreItemRow}>
                {(champ.recommendedItems ?? []).slice(0, 3).map((itemId) => (
                  <div key={itemId} className={styles.coreItemEntry}>
                    <img
                      src={tftItemIconUrl(itemId)}
                      alt={getItemName(itemId, locale)}
                      className={styles.coreItemIcon}
                      onError={tftItemIconOnError}
                    />
                    <span className={styles.coreItemName}>{getItemName(itemId, locale)}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export default ItemsPanel
