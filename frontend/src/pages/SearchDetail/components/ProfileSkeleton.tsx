import styles from './ProfileSkeleton.module.css'

export default function ProfileSkeleton() {
  return (
    <section className={styles.profileSkeleton}>
      <div className={styles.skeletonCircle} />
      <div className={styles.skeletonBlock}>
        <div className={`${styles.skeletonLine} ${styles.skeletonLineWide}`} />
        <div className={`${styles.skeletonLine} ${styles.skeletonLineMid}`} />
        <div className={`${styles.skeletonLine} ${styles.skeletonLineNarrow}`} />
      </div>
    </section>
  )
}
