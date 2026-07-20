import { useEffect, useState, type SyntheticEvent } from 'react'
import { tftItemIconOnError } from '../../../api/communityDragonAssets'
import styles from './ChampionCard.module.css'

const costClasses: Record<number, string> = {
  1: styles.cost1,
  2: styles.cost2,
  3: styles.cost3,
  4: styles.cost4,
  5: styles.cost5,
}

export interface ChampionCardProps {
  label: string
  imageUrl?: string
  items?: {
    imageUrl: string
    name: string
  }[]
  stars?: 1 | 2 | 3
  hasItem?: boolean
  toneIndex?: number
  cost?: number
}

function ChampionCard({ label, imageUrl, items = [], stars = 2, hasItem = false, cost }: ChampionCardProps) {
  const costClass = cost != null ? (costClasses[cost] ?? styles.cost1) : undefined
  const allItems = items.slice(0, 3)
  const [imageFailed, setImageFailed] = useState(false)
  const [failedItemKeys, setFailedItemKeys] = useState<Set<string>>(new Set())

  // imageUrl이 바뀌면 실패 상태 초기화 (새 이미지 재시도)
  useEffect(() => {
    setImageFailed(false)
  }, [imageUrl])

  // items가 바뀌면 실패 상태 초기화 (새 아이템 재시도)
  useEffect(() => {
    setFailedItemKeys(new Set())
  }, [items])

  const equippedItems = allItems
    .map((item, index) => ({ item, itemKey: `${item.name}_${index}` }))
    .filter(({ itemKey }) => !failedItemKeys.has(itemKey))

  function handleItemError(e: SyntheticEvent<HTMLImageElement>, itemKey: string) {
    tftItemIconOnError(e)
    // 모든 fallback 경로가 실패하면 이미지를 완전히 제거해 배경색 박스가 남지 않도록 한다
    if (e.currentTarget.style.opacity === '0') {
      setFailedItemKeys((prev) => new Set(prev).add(itemKey))
    }
  }

  return (
    <span className={`${styles.card} ${costClass ?? ''}`}>
      <small className={styles.stars}>{'★'.repeat(Math.min(stars, 3))}</small>
      {imageUrl && !imageFailed ? (
        <img
          src={imageUrl}
          alt={label}
          className={styles.image}
          onError={() => setImageFailed(true)}
        />
      ) : (
        <b className={styles.label}>{label}</b>
      )}
      {equippedItems.length > 0 && (
        <span className={styles.itemTray}>
          {equippedItems.map(({ item, itemKey }) => (
            <img
              className={styles.itemIcon}
              src={item.imageUrl}
              alt={item.name}
              key={itemKey}
              onError={(e) => handleItemError(e, itemKey)}
            />
          ))}
        </span>
      )}
      {equippedItems.length === 0 && hasItem && <i className={styles.itemMark} />}
    </span>
  )
}

export default ChampionCard
