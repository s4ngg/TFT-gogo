import styles from './ChampionCard.module.css'

const toneClasses = [styles.tone0, styles.tone1, styles.tone2, styles.tone3, styles.tone4, styles.tone5]

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
}

function ChampionCard({ label, imageUrl, items = [], stars = 2, hasItem = false, toneIndex = 0 }: ChampionCardProps) {
  const toneClass = toneClasses[toneIndex % toneClasses.length]
  const equippedItems = items.slice(0, 3)

  return (
    <span className={`${styles.card} ${toneClass}`}>
      <small className={styles.stars}>{'★'.repeat(stars)}</small>
      {imageUrl ? <img src={imageUrl} alt={label} className={styles.image} /> : <b className={styles.label}>{label}</b>}
      {equippedItems.length > 0 && (
        <span className={styles.itemTray}>
          {equippedItems.map((item) => (
            <img className={styles.itemIcon} src={item.imageUrl} alt={item.name} key={item.name} />
          ))}
        </span>
      )}
      {equippedItems.length === 0 && hasItem && <i className={styles.itemMark} />}
    </span>
  )
}

export default ChampionCard
